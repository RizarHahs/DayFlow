package com.dayflow.app.platform.notify

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.dayflow.app.R
import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.RoadmapRepository
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.usecase.Reminder
import com.dayflow.app.domain.usecase.ReminderCategory
import com.dayflow.app.domain.usecase.RemindersPlan
import com.dayflow.app.platform.Channels
import com.dayflow.app.platform.PendingIntents
import com.dayflow.app.platform.setup.SetupChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Habit / task / activity reminders with ONE shared exact alarm: always armed for the earliest
 * reminder not yet shown. When it fires we post whatever is due, remember it (never twice), re-plan.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val habits: HabitRepository,
    private val tasks: TaskRepository,
    private val roadmap: RoadmapRepository,
    private val prefs: AppPrefs,
    private val clock: DayClock,
    private val setup: SetupChecker,
) {
    private val am: AlarmManager get() = ctx.getSystemService<AlarmManager>()!!

    private suspend fun plan(): List<Reminder> {
        val today = clock.today()
        val s = prefs.notificationsNow()
        if (s.enabledCategories.isEmpty()) return emptyList()
        return RemindersPlan.build(
            today = today,
            zone = ZoneId.systemDefault(),
            habits = habits.observeHabits(today).first(),
            tasks = tasks.observeToday(today).first() + tasks.observeUpcoming(today).first(),
            items = roadmap.observeItems().first(),
            logsByDay = mapOf(
                today to roadmap.observeDayLogs(today).first(),
                today + 1 to roadmap.observeDayLogs(today + 1).first(),
            ),
            leadMinutes = s.activityLeadMin,
            enabled = s.enabledCategories,
            reviewMinute = s.reviewMinute,
        )
    }

    suspend fun reschedule() {
        val now = System.currentTimeMillis()
        val posted = prefs.postedKeys()
        val next = plan().firstOrNull { it.key !in posted && it.atMillis > now }
        val pi = PendingIntents.reminderFire(ctx)
        if (next == null) { am.cancel(pi); return }
        try {
            if (setup.canScheduleExact()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, pi)
        }
    }

    /** Alarm fired: post what is due now (within a 15 min grace), then plan the next one. */
    suspend fun onFire() {
        val now = System.currentTimeMillis()
        val posted = prefs.postedKeys()
        val due = plan().filter { it.key !in posted && it.atMillis <= now + 1_000 && it.atMillis > now - GRACE_MS }
        if (due.isNotEmpty()) {
            if (NotificationManagerCompat.from(ctx).areNotificationsEnabled()) due.forEach { post(it) }
            prefs.markPosted(due.map { it.key }, clock.today())
        }
        reschedule()
    }

    private fun post(r: Reminder) {
        val channel = when (r.category) {
            ReminderCategory.HABIT -> Channels.HABITS
            ReminderCategory.TASK -> Channels.TASKS
            ReminderCategory.ACTIVITY -> Channels.ACTIVITIES
            ReminderCategory.REVIEW -> Channels.REVIEW
        }
        val b = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.title)
            .setContentText(r.body)
            .setAutoCancel(true)
            .setContentIntent(PendingIntents.openApp(ctx, if (r.category == ReminderCategory.REVIEW) "review/${r.day}" else null))
        when (r.category) {
            ReminderCategory.HABIT -> b.addAction(0, "Done", PendingIntents.reminderDone(ctx, "H", r.targetId, r.day))
            ReminderCategory.TASK -> b.addAction(0, "Done", PendingIntents.reminderDone(ctx, "T", r.targetId, r.day))
            ReminderCategory.ACTIVITY, ReminderCategory.REVIEW -> Unit
        }
        try { ctx.getSystemService<NotificationManager>()?.notify(notificationId(r.key), b.build()) } catch (_: SecurityException) {}
    }

    companion object {
        private const val GRACE_MS = 15 * 60_000L
        fun notificationId(key: String): Int = 8000 + (key.hashCode() and 0x7FF)
    }
}
