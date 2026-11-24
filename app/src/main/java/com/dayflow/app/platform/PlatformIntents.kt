package com.dayflow.app.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dayflow.app.MainActivity
import com.dayflow.app.platform.alarm.AlarmReceiver
import com.dayflow.app.platform.notify.ReminderReceiver
import com.dayflow.app.platform.timer.TimerReceiver
import com.dayflow.app.platform.whatsapp.OpenWhatsAppActivity
import com.dayflow.app.platform.whatsapp.ScheduledMessageReceiver

object Actions {
    const val ALARM_FIRE = "com.dayflow.app.ALARM_FIRE"
    const val ALARM_SNOOZE = "com.dayflow.app.ALARM_SNOOZE"
    const val ALARM_DISMISS = "com.dayflow.app.ALARM_DISMISS"
    const val ALARM_START_RING = "com.dayflow.app.ALARM_START_RING"
    const val ALARM_STOPPED = "com.dayflow.app.ALARM_STOPPED"

    const val TIMER_FINISH = "com.dayflow.app.TIMER_FINISH"
    const val TIMER_PAUSE = "com.dayflow.app.TIMER_PAUSE"
    const val TIMER_RESUME = "com.dayflow.app.TIMER_RESUME"
    const val TIMER_CANCEL = "com.dayflow.app.TIMER_CANCEL"
    const val TIMER_DISMISS = "com.dayflow.app.TIMER_DISMISS"

    const val MESSAGE_FIRE = "com.dayflow.app.MESSAGE_FIRE"
    const val REMINDER_FIRE = "com.dayflow.app.REMINDER_FIRE"
    const val REMINDER_DONE = "com.dayflow.app.REMINDER_DONE"

    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SNOOZE_FIRE = "snooze_fire"
    const val EXTRA_LABEL = "label"
    const val EXTRA_MINUTE = "minute"
    const val EXTRA_SOUND = "sound"
    const val EXTRA_VIBRATE = "vibrate"
    const val EXTRA_SNOOZE_MIN = "snooze_min"
    const val EXTRA_TIMER_ID = "timer_id"
    const val EXTRA_KIND = "kind"
    const val EXTRA_TARGET_ID = "target_id"
    const val EXTRA_DAY = "day"
    const val EXTRA_ROUTE = "route"
    const val EXTRA_MESSAGE_ID = "message_id"
    const val EXTRA_SUBTEXT = "subtext"
    const val EXTRA_TEXT = "text"
}

/**
 * Every PendingIntent in the app is built here, with FLAG_IMMUTABLE and a request code that is
 * unique per purpose + id so alarms/timers never overwrite each other and can be cancelled exactly.
 */
object PendingIntents {
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    // request-code bands
    private const val RC_ALARM = 10_000
    private const val RC_SNOOZE = 20_000
    private const val RC_TIMER_FINISH = 30_000
    private const val RC_TIMER_ACTION = 40_000
    private const val RC_REMINDER = 50_000
    private const val RC_OPEN_APP = 60_000
    private const val RC_SHOW_ALARM = 70_000
    private const val RC_TEST = 80_000
    private const val RC_MESSAGE_FIRE = 90_000
    private const val RC_OPEN_WHATSAPP = 100_000

    fun rc(base: Int, id: Long): Int = base + (id % 9_000).toInt().let { if (it < 0) -it else it }

    /** [route] (e.g. "review/20400") makes the app open straight to that screen. */
    fun openApp(ctx: Context, route: String? = null): PendingIntent = PendingIntent.getActivity(
        ctx, if (route == null) RC_OPEN_APP else RC_OPEN_APP + 1 + (route.hashCode() and 0x3FF),
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .also { if (route != null) it.putExtra(Actions.EXTRA_ROUTE, route) },
        FLAGS,
    )

    /** Shown by the system next to the alarm-clock icon. */
    fun showAlarm(ctx: Context, id: Long): PendingIntent = PendingIntent.getActivity(
        ctx, rc(RC_SHOW_ALARM, id), Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FLAGS,
    )

    fun alarmFire(ctx: Context, id: Long, snooze: Boolean = false): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).setAction(Actions.ALARM_FIRE)
            .putExtra(Actions.EXTRA_ALARM_ID, id).putExtra(Actions.EXTRA_SNOOZE_FIRE, snooze)
        val base = when { id < 0 -> RC_TEST; snooze -> RC_SNOOZE; else -> RC_ALARM }
        return PendingIntent.getBroadcast(ctx, rc(base, id), i, FLAGS)
    }

    fun timerFinish(ctx: Context, id: Long): PendingIntent = PendingIntent.getBroadcast(
        ctx, rc(RC_TIMER_FINISH, id),
        Intent(ctx, TimerReceiver::class.java).setAction(Actions.TIMER_FINISH).putExtra(Actions.EXTRA_TIMER_ID, id), FLAGS,
    )

    fun timerAction(ctx: Context, action: String, id: Long): PendingIntent = PendingIntent.getBroadcast(
        ctx, rc(RC_TIMER_ACTION + action.hashCode().and(0xFF) * 10, id),
        Intent(ctx, TimerReceiver::class.java).setAction(action).putExtra(Actions.EXTRA_TIMER_ID, id), FLAGS,
    )

    /** One shared alarm drives all reminders; the receiver re-plans each time. */
    fun reminderFire(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, RC_REMINDER, Intent(ctx, ReminderReceiver::class.java).setAction(Actions.REMINDER_FIRE), FLAGS,
    )

    fun reminderDone(ctx: Context, kind: String, targetId: Long, day: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, rc(RC_REMINDER + 1_000 + kind.hashCode().and(0x7), targetId * 31 + day),
        Intent(ctx, ReminderReceiver::class.java).setAction(Actions.REMINDER_DONE)
            .putExtra(Actions.EXTRA_KIND, kind).putExtra(Actions.EXTRA_TARGET_ID, targetId).putExtra(Actions.EXTRA_DAY, day),
        FLAGS,
    )

    /** Exact alarm for one scheduled message. Request code is per message, so edits/deletes cancel precisely that alarm. */
    fun messageFire(ctx: Context, messageId: Long): PendingIntent = PendingIntent.getBroadcast(
        ctx, rc(RC_MESSAGE_FIRE, messageId),
        Intent(ctx, ScheduledMessageReceiver::class.java).setAction(Actions.MESSAGE_FIRE)
            .putExtra(Actions.EXTRA_MESSAGE_ID, messageId),
        FLAGS,
    )

    /**
     * Goes straight to an Activity (never via a receiver or service): Android 12+ blocks notification
     * "trampolines", and this is also what lets WhatsApp open when the user taps the notification.
     */
    fun openWhatsApp(ctx: Context, messageId: Long): PendingIntent = PendingIntent.getActivity(
        ctx, rc(RC_OPEN_WHATSAPP, messageId), OpenWhatsAppActivity.intent(ctx, messageId), FLAGS,
    )
}
