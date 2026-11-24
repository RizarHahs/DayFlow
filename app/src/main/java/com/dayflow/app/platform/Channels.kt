package com.dayflow.app.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.content.getSystemService

/** Notification channels. Users can tune each one in system settings; we only create them. */
object Channels {
    const val ALARMS = "alarms"                 // ring service plays its own sound
    const val ALARMS_MISSED = "alarms_missed"
    const val TIMER_DONE = "timer_done"
    const val TIMER_RUNNING = "timer_running"
    const val HABITS = "habit_reminders"
    const val TASKS = "task_reminders"
    const val ACTIVITIES = "activity_reminders"
    const val REVIEW = "daily_review"
    const val MESSAGES = "scheduled_messages"

    fun ensure(ctx: Context) {
        val nm = ctx.getSystemService<NotificationManager>() ?: return
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        nm.createNotificationChannel(
            NotificationChannel(ALARMS, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Ringing alarms"
                setSound(null, null)          // the ring service plays sound itself, on the alarm stream
                enableVibration(false)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(ALARMS_MISSED, "Missed alarms", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you when an alarm rang while you were away or the phone was off"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(TIMER_DONE, "Timer finished", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "When a timer reaches zero"
                setSound(alarmSound, alarmAttrs)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(TIMER_RUNNING, "Running timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Silent notification with the live countdown"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(HABITS, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(TASKS, "Task reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(ACTIVITIES, "Upcoming activities", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(REVIEW, "Daily review", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(MESSAGES, "Scheduled messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tells you when it is time to send a WhatsApp message you scheduled"
            }
        )
    }
}
