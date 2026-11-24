package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.time.isScheduled
import com.dayflow.app.domain.time.toLocalDate
import java.time.ZoneId

enum class ReminderCategory { HABIT, TASK, ACTIVITY, REVIEW }

data class Reminder(
    val key: String,          // stable per (kind, id, day): used to never notify twice
    val atMillis: Long,
    val title: String,
    val body: String,
    val category: ReminderCategory,
    val targetId: Long,
    val day: Int,
)

/**
 * Pure planner: which reminders exist for today and tomorrow. The platform layer schedules the
 * earliest unposted one, posts what is due, then plans again. Nothing here touches Android.
 */
object RemindersPlan {
    fun atMillis(day: Int, minute: Int, zone: ZoneId): Long =
        day.toLocalDate().atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()

    fun build(
        today: Int,
        zone: ZoneId,
        habits: List<Habit>,
        tasks: List<TaskItem>,
        items: List<RoadmapItem>,
        logsByDay: Map<Int, List<RoadmapDayLog>>,
        leadMinutes: Int,
        enabled: Set<ReminderCategory>,
        reviewMinute: Int = 21 * 60 + 30,
    ): List<Reminder> {
        val out = mutableListOf<Reminder>()
        for (d in today..today + 1) {
            if (ReminderCategory.HABIT in enabled) {
                habits.filter { it.reminderMinute != null && HabitSchedule.isDue(it.repeatMask, it.createdDay, null, d) }
                    .filter { !(d == today && it.done) }
                    .forEach {
                        out += Reminder("H:${it.id}:$d", atMillis(d, it.reminderMinute!!, zone), it.title, "Time for your habit", ReminderCategory.HABIT, it.id, d)
                    }
            }
            if (ReminderCategory.REVIEW in enabled) {
                out += Reminder("R:0:$d", atMillis(d, reviewMinute, zone), "Review your day", "See how today went and jot a note", ReminderCategory.REVIEW, 0L, d)
            }
            if (ReminderCategory.TASK in enabled) {
                tasks.filter { !it.done && it.reminderMinute != null && it.plannedDay == d }
                    .forEach {
                        out += Reminder("T:${it.id}:$d", atMillis(d, it.reminderMinute!!, zone), it.title, "Task reminder", ReminderCategory.TASK, it.id, d)
                    }
            }
            if (ReminderCategory.ACTIVITY in enabled) {
                val logs = logsByDay[d].orEmpty().associateBy { it.itemId }
                items.filter { isScheduled(it.repeatMask, d) }.forEach { item ->
                    val log = logs[item.id]
                    if (log?.status == DayLogStatus.DONE || log?.status == DayLogStatus.SKIPPED) return@forEach
                    val start = log?.overrideStart ?: item.startMinute
                    val at = (start - leadMinutes).coerceAtLeast(0)
                    val body = if (leadMinutes > 0) "Starts in $leadMinutes min" else "Starting now"
                    out += Reminder("A:${item.id}:$d", atMillis(d, at, zone), item.title, body, ReminderCategory.ACTIVITY, item.id, d)
                }
            }
        }
        return out.sortedBy { it.atMillis }
    }
}
