package com.dayflow.app.platform.whatsapp

import android.app.AlarmManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.dayflow.app.data.local.ScheduledMessageDao
import com.dayflow.app.data.local.ScheduledMessageEntity
import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.domain.model.MessageStatus
import com.dayflow.app.domain.usecase.RemindersPlan
import com.dayflow.app.platform.PendingIntents
import com.dayflow.app.platform.alarm.AlarmRingService
import com.dayflow.app.platform.setup.SetupChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arms one exact AlarmManager alarm per scheduled message and handles delivery.
 *
 *  - Normal mode: setExactAndAllowWhileIdle (not shown as a clock alarm).
 *  - Attention Mode: setAlarmClock, which Android exempts from Doze, plus the alarm ring pipeline.
 *  - Without exact-alarm access it falls back to an inexact alarm and reports "not guaranteed".
 *
 * Alarms live only in AlarmManager (lost on reboot); the truth is the Room row, and [rescheduleAll] rebuilds
 * the alarms from it after reboot, app update, clock/timezone change and permission change.
 */
@Singleton
class ScheduledMessageScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val dao: ScheduledMessageDao,
    private val prefs: AppPrefs,
    private val setup: SetupChecker,
    private val notifier: MessageNotifier,
) {
    private val am: AlarmManager get() = ctx.getSystemService<AlarmManager>()!!

    fun triggerAt(m: ScheduledMessageEntity): Long =
        RemindersPlan.atMillis(m.day, m.minute, ZoneId.systemDefault())

    /**
     * Arms the alarm for a SCHEDULED message. A time that already passed (phone was off, app was force-stopped)
     * is delivered immediately and marked as late instead of being silently dropped.
     * @return true only if Android accepted an EXACT alarm (or the message was delivered right now).
     */
    suspend fun schedule(m: ScheduledMessageEntity): Boolean {
        if (m.status != MessageStatus.SCHEDULED) { cancel(m.id); return true }
        val at = triggerAt(m)
        if (at <= System.currentTimeMillis()) { onFire(m.id, late = true); return true }

        val pi = PendingIntents.messageFire(ctx, m.id)
        if (!setup.canScheduleExact()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            return false
        }
        return try {
            if (prefs.messageSettingsNow().attentionMode) {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(at, PendingIntents.openApp(ctx, "messages")), pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
            true
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            false
        }
    }

    fun cancel(id: Long) {
        am.cancel(PendingIntents.messageFire(ctx, id))
    }

    /** Re-arms every SCHEDULED message from the database. Idempotent, so it is safe to call often. */
    suspend fun rescheduleAll() {
        dao.scheduled().forEach { schedule(it) }
    }

    /**
     * Alarm fired (or a late message is being delivered). The SCHEDULED -> TRIGGERED update is atomic, so
     * duplicate deliveries, edited messages and deleted messages produce no duplicate and no stray alert.
     */
    suspend fun onFire(id: Long, late: Boolean = false) {
        val m = dao.get(id) ?: return                                   // deleted or invalid: nothing to do
        val now = System.currentTimeMillis()
        if (dao.markTriggered(id, now) == 0) return                     // already handled / edited / opened

        val scheduledFor = triggerAt(m)
        val wasLate = late || now - scheduledFor > LATE_AFTER_MS
        val time = LocalTime.of(m.minute / 60, m.minute % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
        val title = "Time to message ${m.displayNameForAlert()}"
        val sub = if (wasLate) "${m.occasion} · was scheduled for $time" else "${m.occasion} · $time"

        val settings = prefs.messageSettingsNow()
        val delivered = if (settings.attentionMode) {
            try {
                ContextCompat.startForegroundService(ctx, AlarmRingService.messageIntent(ctx, m.id, title, sub, m.message))
                true
            } catch (_: Exception) {
                // e.g. Android refused to start a foreground service from the background: fall back to a notification
                notifier.postRaw(m.id, title, sub, m.message)
            }
        } else {
            notifier.postRaw(m.id, title, sub, m.message)
        }
        if (!delivered) {
            dao.markFailed(id, "Notifications are turned off, so no alert could be shown. Open it from DAYFLOW.")
        }
    }

    private fun ScheduledMessageEntity.displayNameForAlert() = recipientName.ifBlank { phone }

    companion object { private const val LATE_AFTER_MS = 2 * 60_000L }
}
