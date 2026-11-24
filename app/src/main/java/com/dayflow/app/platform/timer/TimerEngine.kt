package com.dayflow.app.platform.timer

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.dayflow.app.R
import com.dayflow.app.data.local.TimerDao
import com.dayflow.app.data.local.TimerSessionEntity
import com.dayflow.app.data.repo.toDomain
import com.dayflow.app.domain.model.TimerSession
import com.dayflow.app.domain.model.TimerState
import com.dayflow.app.domain.model.TimerTarget
import com.dayflow.app.domain.repo.TimerController
import com.dayflow.app.platform.Actions
import com.dayflow.app.platform.Channels
import com.dayflow.app.platform.PendingIntents
import com.dayflow.app.platform.setup.SetupChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Countdown timers that survive app close, process death and reboot.
 *
 * Source of truth = persisted timestamps (never a ticking counter):
 *   start:  endElapsed = elapsedRealtime() + duration   (monotonic, used while the boot is unchanged)
 *           endEpoch   = now + duration                  (wall clock, used only after a reboot)
 * Finish is delivered by an exact AlarmManager alarm; the running notification uses the system
 * chronometer, so no always-on service exists for OxygenOS to kill. One timer at a time by design.
 */
@Singleton
class TimerEngine @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val dao: TimerDao,
    private val setup: SetupChecker,
) : TimerController {

    private val am: AlarmManager get() = ctx.getSystemService<AlarmManager>()!!
    private val nm: NotificationManager? get() = ctx.getSystemService<NotificationManager>()
    private val lock = Mutex()

    override val current: Flow<TimerSession?> = dao.observeCurrent().map { it?.toDomain() }

    override fun bootCount(): Int = Settings.Global.getInt(ctx.contentResolver, Settings.Global.BOOT_COUNT, 0)

    private fun remaining(e: TimerSessionEntity): Long = e.toDomain()
        .remainingMs(SystemClock.elapsedRealtime(), System.currentTimeMillis(), bootCount())

    // ---- public API ----

    override suspend fun start(label: String, durationMs: Long, target: TimerTarget?, targetId: Long?) {
        lock.withLock {
        closeActive(TimerState.CANCELLED)
        val nowE = SystemClock.elapsedRealtime()
        val nowW = System.currentTimeMillis()
        val e = TimerSessionEntity(
            label = label.trim(), targetType = target, targetId = targetId, durationMs = durationMs,
            state = TimerState.RUNNING, startedElapsed = nowE, endElapsed = nowE + durationMs,
            endEpoch = nowW + durationMs, remainingMsPaused = 0, bootCount = bootCount(),
            startedAt = nowW, focusMs = 0,
        )
        val saved = e.copy(id = dao.insert(e))
        armFinish(saved)
        postRunning(saved)
        }
    }

    override suspend fun pause() {
        lock.withLock {
        val e = dao.current()?.takeIf { it.state == TimerState.RUNNING } ?: return@withLock
        val left = remaining(e)
        val paused = e.copy(state = TimerState.PAUSED, remainingMsPaused = left)
        dao.update(paused)
        am.cancel(PendingIntents.timerFinish(ctx, e.id))
        postPaused(paused)
        }
    }

    override suspend fun resume() {
        lock.withLock {
        val e = dao.current()?.takeIf { it.state == TimerState.PAUSED } ?: return@withLock
        reanchor(e, e.remainingMsPaused)
        }
    }

    override suspend fun cancel() {
        lock.withLock { closeActive(TimerState.CANCELLED) }
    }

    override suspend fun reset() {
        val e = dao.current() ?: return
        start(e.label, e.durationMs, e.targetType, e.targetId)
    }

    override suspend fun dismissFinished() {
        lock.withLock {
            val e = dao.current() ?: return@withLock
            if (e.state == TimerState.FINISHED) dao.update(e.copy(state = TimerState.DISMISSED))
            nm?.cancel(NOTIF_DONE)
        }
    }

    // ---- platform entry points (receivers / app start / boot) ----

    /** Alarm fired for [id]. Ignored if that session was paused/cancelled in the meantime. */
    suspend fun onFinishAlarm(id: Long) {
        lock.withLock {
            val e = dao.get(id) ?: return@withLock
            if (e.state != TimerState.RUNNING) return@withLock
            finish(e, wasOffline = false)
        }
    }

    /**
     * Self-heal after process death, reboot, update, or clock/timezone changes. A running timer is
     * re-anchored to the current boot's monotonic clock; one that expired while we were gone finishes now.
     */
    suspend fun restore() {
        lock.withLock {
        val e = dao.current() ?: return@withLock
        when (e.state) {
            TimerState.RUNNING -> {
                val left = remaining(e)
                if (left <= 0L) finish(e, wasOffline = true) else reanchor(e, left)
            }
            TimerState.PAUSED -> postPaused(e)
            else -> Unit
        }
        }
    }

    // ---- internals ----

    private suspend fun reanchor(e: TimerSessionEntity, leftMs: Long) {
        val nowE = SystemClock.elapsedRealtime()
        val nowW = System.currentTimeMillis()
        val running = e.copy(
            state = TimerState.RUNNING, endElapsed = nowE + leftMs, endEpoch = nowW + leftMs,
            remainingMsPaused = 0, bootCount = bootCount(),
        )
        dao.update(running)
        armFinish(running)
        postRunning(running)
    }

    private suspend fun finish(e: TimerSessionEntity, wasOffline: Boolean) {
        dao.update(e.copy(state = TimerState.FINISHED, remainingMsPaused = 0, focusMs = e.durationMs))
        am.cancel(PendingIntents.timerFinish(ctx, e.id))
        nm?.cancel(NOTIF_RUNNING)
        postDone(e, wasOffline)
    }

    private suspend fun closeActive(newState: TimerState) {
        val e = dao.current() ?: return
        if (e.state == TimerState.RUNNING || e.state == TimerState.PAUSED) {
            val left = if (e.state == TimerState.RUNNING) remaining(e) else e.remainingMsPaused
            dao.update(e.copy(state = newState, remainingMsPaused = 0, focusMs = (e.durationMs - left).coerceIn(0, e.durationMs)))
        } else {
            dao.update(e.copy(state = TimerState.DISMISSED))
        }
        am.cancel(PendingIntents.timerFinish(ctx, e.id))
        nm?.cancel(NOTIF_RUNNING)
        nm?.cancel(NOTIF_DONE)
    }

    private fun armFinish(e: TimerSessionEntity) {
        val pi = PendingIntents.timerFinish(ctx, e.id)
        val exact = setup.canScheduleExact()
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, e.endElapsed, pi)
            else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, e.endElapsed, pi)   // may be late; UI says so
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, e.endElapsed, pi)
        }
    }

    private fun title(e: TimerSessionEntity) = e.label.ifBlank { "Timer" }

    private fun postRunning(e: TimerSessionEntity) {
        val left = (e.endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        val n = NotificationCompat.Builder(ctx, Channels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title(e))
            .setContentText("Running")
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setUsesChronometer(true).setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + left)
            .setContentIntent(PendingIntents.openApp(ctx))
            .addAction(0, "Pause", PendingIntents.timerAction(ctx, Actions.TIMER_PAUSE, e.id))
            .addAction(0, "Stop", PendingIntents.timerAction(ctx, Actions.TIMER_CANCEL, e.id))
            .build()
        safeNotify(NOTIF_RUNNING, n)
    }

    private fun postPaused(e: TimerSessionEntity) {
        val n = NotificationCompat.Builder(ctx, Channels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title(e))
            .setContentText("Paused · ${format(e.remainingMsPaused)} left")
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setContentIntent(PendingIntents.openApp(ctx))
            .addAction(0, "Resume", PendingIntents.timerAction(ctx, Actions.TIMER_RESUME, e.id))
            .addAction(0, "Stop", PendingIntents.timerAction(ctx, Actions.TIMER_CANCEL, e.id))
            .build()
        safeNotify(NOTIF_RUNNING, n)
    }

    private fun postDone(e: TimerSessionEntity, wasOffline: Boolean) {
        val n = NotificationCompat.Builder(ctx, Channels.TIMER_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${title(e)} finished")
            .setContentText(if (wasOffline) "It ran out while the phone or app wasn't running." else "Time's up.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(PendingIntents.openApp(ctx))
            .addAction(0, "Dismiss", PendingIntents.timerAction(ctx, Actions.TIMER_DISMISS, e.id))
            .build()
        safeNotify(NOTIF_DONE, n)
    }

    private fun safeNotify(id: Int, n: android.app.Notification) {
        try { nm?.notify(id, n) } catch (_: SecurityException) { /* POST_NOTIFICATIONS denied: the in-app timer still works */ }
    }

    private fun format(ms: Long): String {
        val s = (ms + 999) / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    companion object {
        const val NOTIF_RUNNING = 7002
        const val NOTIF_DONE = 7003
    }
}
