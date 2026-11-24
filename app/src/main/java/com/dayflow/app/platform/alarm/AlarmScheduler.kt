package com.dayflow.app.platform.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.dayflow.app.R
import com.dayflow.app.data.local.AlarmDao
import com.dayflow.app.data.local.AlarmEntity
import com.dayflow.app.domain.time.NextTrigger
import com.dayflow.app.platform.Actions
import com.dayflow.app.platform.Channels
import com.dayflow.app.platform.PendingIntents
import com.dayflow.app.platform.setup.SetupChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns every real AlarmManager call for user alarms.
 *
 * Decision (Phase 1): AlarmManager.setAlarmClock(). It is exempt from Doze, shows the system alarm
 * icon and is what OxygenOS treats as a wake-up alarm. If exact-alarm access is missing the call is
 * not attempted and the alarm is reported as NOT guaranteed instead of silently pretending.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val dao: AlarmDao,
    private val setup: SetupChecker,
) {
    private val am: AlarmManager get() = ctx.getSystemService<AlarmManager>()!!

    /** @return true if the OS accepted an exact alarm; false = saved but not guaranteed. */
    fun scheduleAt(alarmId: Long, triggerAt: Long, snooze: Boolean = false): Boolean {
        if (!setup.canScheduleExact()) return false
        return try {
            am.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, PendingIntents.showAlarm(ctx, alarmId)),
                PendingIntents.alarmFire(ctx, alarmId, snooze),
            )
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm refused by OS", e)
            false
        }
    }

    fun cancel(alarmId: Long) {
        am.cancel(PendingIntents.alarmFire(ctx, alarmId, snooze = false))
        am.cancel(PendingIntents.alarmFire(ctx, alarmId, snooze = true))
    }

    /** Computes the next occurrence, stores it, and arms the OS alarm. Disabled alarms are cancelled. */
    suspend fun schedule(alarm: AlarmEntity): Boolean {
        if (!alarm.enabled) {
            cancel(alarm.id)
            dao.update(alarm.copy(nextTriggerAt = null))
            return true
        }
        val next = NextTrigger.compute(alarm.minute, alarm.repeatMask, System.currentTimeMillis(), ZoneId.systemDefault())
        dao.update(alarm.copy(nextTriggerAt = next))
        return scheduleAt(alarm.id, next)
    }

    /**
     * Re-arms everything: after reboot, app update, clock/timezone change and every app start (self-heal).
     * With [reportMissed], alarms whose trigger time passed while we were not running are surfaced
     * as "Missed alarm" notifications instead of being silently dropped.
     */
    suspend fun rescheduleAll(reportMissed: Boolean = false) {
        val now = System.currentTimeMillis()
        dao.getAll().filter { it.enabled }.forEach { a ->
            val due = a.nextTriggerAt
            if (reportMissed && due != null && due < now - GRACE_MS) postMissed(a, due)
            schedule(a)
        }
    }

    /** Called by the receiver when an alarm fires. Starts the ring service, then arms the next occurrence. */
    suspend fun onFired(alarmId: Long, isSnooze: Boolean) {
        if (alarmId == TEST_ID) { startRing(TEST_ID, "Test alarm", minuteOfNow(), null, true, 1); return }
        val a = dao.get(alarmId) ?: return
        if (!a.enabled && !isSnooze) return
        startRing(a.id, a.label.ifBlank { a.kind.label }, a.minute, a.soundUri, a.vibrate, a.snoozeMin)
        if (!isSnooze) {
            if (a.repeatMask == 0) {
                dao.update(a.copy(enabled = false, nextTriggerAt = null))   // one-shot: done
            } else {
                schedule(a)                                                 // repeating: arm the next one now
            }
        }
    }

    fun scheduleSnooze(alarmId: Long, minutes: Int) {
        scheduleAt(alarmId, System.currentTimeMillis() + minutes * 60_000L, snooze = true)
    }

    fun scheduleTest(delayMs: Long): Boolean = scheduleAt(TEST_ID, System.currentTimeMillis() + delayMs)

    private fun minuteOfNow(): Int = java.time.LocalTime.now().let { it.hour * 60 + it.minute }

    private fun startRing(id: Long, label: String, minute: Int, sound: String?, vibrate: Boolean, snoozeMin: Int) {
        ContextCompat.startForegroundService(
            ctx, AlarmRingService.startIntent(ctx, id, label, minute, sound, vibrate, snoozeMin)
        )
    }

    private fun postMissed(a: AlarmEntity, due: Long) {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        val time = "%02d:%02d".format(a.minute / 60, a.minute % 60)
        val n = NotificationCompat.Builder(ctx, Channels.ALARMS_MISSED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Missed alarm")
            .setContentText("${a.label.ifBlank { a.kind.label }} · $time (the app wasn't running or the phone was off)")
            .setContentIntent(PendingIntents.openApp(ctx))
            .setAutoCancel(true)
            .build()
        try { ctx.getSystemService<NotificationManager>()?.notify(MISSED_BASE + (a.id % 500).toInt(), n) } catch (_: SecurityException) {}
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        const val TEST_ID = -1L
        private const val MISSED_BASE = 7100
        private const val GRACE_MS = 2 * 60_000L
    }
}
