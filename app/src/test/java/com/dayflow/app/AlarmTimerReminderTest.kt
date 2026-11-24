package com.dayflow.app

import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.time.*
import com.dayflow.app.domain.usecase.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Phase 4 rules: next alarm time (timezone/DST safe), timer remaining time across reboot, reminder planning. */
class AlarmTimerReminderTest {
    private fun check(name: String, ok: Boolean) = assertTrue(name, ok)

    @Test
    fun alarmTimerAndReminderRules() {

        val z = ZoneId.of("Asia/Kathmandu")
        fun ms(y: Int, m: Int, d: Int, h: Int, min: Int) = LocalDateTime.of(y, m, d, h, min).atZone(z).toInstant().toEpochMilli()

        // NextTrigger
        val now = ms(2026, 11, 16, 6, 0) // Monday 06:00
        check("later today", NextTrigger.compute(6 * 60 + 30, ALL_DAYS, now, z) == ms(2026, 11, 16, 6, 30))
        check("already passed -> tomorrow", NextTrigger.compute(5 * 60, ALL_DAYS, now, z) == ms(2026, 11, 17, 5, 0))
        check("exactly now -> next day (strictly after)", NextTrigger.compute(6 * 60, ALL_DAYS, now, z) == ms(2026, 11, 17, 6, 0))
        check("one-shot (mask 0) later today", NextTrigger.compute(7 * 60, 0, now, z) == ms(2026, 11, 16, 7, 0))
        check("weekdays from Friday evening -> Monday", NextTrigger.compute(7 * 60, 0b0011111, ms(2026, 11, 20, 20, 0), z) == ms(2026, 11, 23, 7, 0))
        check("only Sunday", NextTrigger.compute(8 * 60, 0b1000000, now, z) == ms(2026, 11, 22, 8, 0))
        check("timezone matters", NextTrigger.compute(6 * 60 + 30, ALL_DAYS, now, ZoneId.of("UTC")) != ms(2026, 11, 16, 6, 30))

        // Timer remaining
        val t = TimerSession(1, "x", null, null, 60_000, TimerState.RUNNING, endElapsed = 100_000, endEpoch = 5_000_000, remainingMsPaused = 0, bootCount = 7)
        check("running same boot uses elapsed", t.remainingMs(70_000, 0, 7) == 30_000L)
        check("running clamps at 0", t.remainingMs(150_000, 0, 7) == 0L)
        check("reboot falls back to wall clock", t.remainingMs(1_000, 4_970_000, 8) == 30_000L)
        check("wall clock change ignored within boot", t.remainingMs(70_000, 99_999_999, 7) == 30_000L)
        check("paused keeps value", t.copy(state = TimerState.PAUSED, remainingMsPaused = 12_345).remainingMs(0, 0, 7) == 12_345L)
        check("finished is zero", t.copy(state = TimerState.FINISHED).remainingMs(0, 0, 7) == 0L)

        // Reminders
        val today = ms(2026, 11, 16, 0, 0).let { Instant.ofEpochMilli(it).atZone(z).toLocalDate().toEpochDay().toInt() }
        fun habit(id: Long, rem: Int?, done: Boolean = false, mask: Int = ALL_DAYS) =
            Habit(id, "H$id", Period.MORNING, null, mask, rem, null, today - 10, done, 0, true)
        val habits = listOf(habit(1, 7 * 60), habit(2, 8 * 60, done = true), habit(3, null))
        val plan = RemindersPlan.build(today, z, habits, emptyList(), emptyList(), emptyMap(), 5, setOf(ReminderCategory.HABIT, ReminderCategory.TASK, ReminderCategory.ACTIVITY))
        check("habit reminder today+tomorrow, done-today skipped", plan.map { it.key } == listOf("H:1:$today", "H:2:${today + 1}", "H:1:${today + 1}").sortedBy { k -> plan.first { it.key == k }.atMillis })
        check("no reminder without time", plan.none { it.targetId == 3L })
        val task = TaskItem(10, TaskKind.IMPORTANT, "T", done = false, plannedDay = today, reminderMinute = 9 * 60)
        val doneTask = task.copy(id = 11, done = true)
        val p2 = RemindersPlan.build(today, z, emptyList(), listOf(task, doneTask), emptyList(), emptyMap(), 5, setOf(ReminderCategory.TASK))
        check("only open task reminded", p2.map { it.targetId } == listOf(10L))
        val item = RoadmapItem(5, "Study", 9 * 60, 10 * 60, ALL_DAYS, null, null)
        val p3 = RemindersPlan.build(today, z, emptyList(), emptyList(), listOf(item), emptyMap(), 10, setOf(ReminderCategory.ACTIVITY))
        check("activity lead time", p3.first().atMillis == RemindersPlan.atMillis(today, 9 * 60 - 10, z) && p3.first().body == "Starts in 10 min")
        val skipLog = mapOf(today to listOf(RoadmapDayLog(5, today, DayLogStatus.SKIPPED, null, null)))
        val p4 = RemindersPlan.build(today, z, emptyList(), emptyList(), listOf(item), skipLog, 10, setOf(ReminderCategory.ACTIVITY))
        check("skipped activity not reminded today but tomorrow yes", p4.map { it.day } == listOf(today + 1))
        val moved = mapOf(today to listOf(RoadmapDayLog(5, today, DayLogStatus.RESCHEDULED, 14 * 60, 15 * 60)))
        val p5 = RemindersPlan.build(today, z, emptyList(), emptyList(), listOf(item), moved, 0, setOf(ReminderCategory.ACTIVITY))
        check("rescheduled uses override", p5.first { it.day == today }.atMillis == RemindersPlan.atMillis(today, 14 * 60, z) && p5.first().body == "Starting now")
        val off = RemindersPlan.build(today, z, habits, listOf(task), listOf(item), emptyMap(), 5, emptySet())
        check("disabled categories produce nothing", off.isEmpty())
        val weekendOnly = listOf(habit(9, 7 * 60, mask = 0b1100000))
        val p6 = RemindersPlan.build(today, z, weekendOnly, emptyList(), emptyList(), emptyMap(), 5, setOf(ReminderCategory.HABIT))
        check("monday/tuesday skip weekend-only habit", p6.isEmpty())


    }
}
