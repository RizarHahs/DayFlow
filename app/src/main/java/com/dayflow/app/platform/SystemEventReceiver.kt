package com.dayflow.app.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayflow.app.platform.alarm.AlarmScheduler
import com.dayflow.app.platform.notify.ReminderScheduler
import com.dayflow.app.platform.timer.TimerEngine
import com.dayflow.app.platform.whatsapp.ScheduledMessageScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Restores everything the OS forgets: reboot, app update, clock / timezone / date changes.
 * Alarms are re-armed (missed ones reported after a reboot), timers re-anchored, reminders re-planned.
 */
@AndroidEntryPoint
class SystemEventReceiver : BroadcastReceiver() {
    @Inject lateinit var alarms: AlarmScheduler
    @Inject lateinit var timers: TimerEngine
    @Inject lateinit var reminders: ReminderScheduler
    @Inject lateinit var messages: ScheduledMessageScheduler

    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action ?: return
        if (action !in HANDLED) return
        runAsync {
            // Each step is isolated: a failure in one must never stop the others from restoring.
            runCatching { alarms.rescheduleAll(reportMissed = action == Intent.ACTION_BOOT_COMPLETED) }
            runCatching { timers.restore() }
            runCatching { reminders.reschedule() }
            runCatching { messages.rescheduleAll() }   // re-arm scheduled WhatsApp messages; missed ones are delivered now
        }
    }

    companion object {
        private val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
