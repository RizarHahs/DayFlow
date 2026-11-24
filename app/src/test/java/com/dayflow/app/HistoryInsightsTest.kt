package com.dayflow.app

import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.time.*
import com.dayflow.app.domain.usecase.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Phase 5 rules: day summaries, habit statistics, streaks, calendar grid, review reminder. */
class HistoryInsightsTest {
    private fun check(name: String, ok: Boolean) = assertTrue(name, ok)
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay().toInt()

    @Test
    fun historyAndInsightsRules() {
        val today = day(2026, 11, 16) // Monday
        val habits = listOf(
            HabitDef(1, "Water", ALL_DAYS, today - 40, null),
            HabitDef(2, "Gym", 0b0010101, today - 40, null),          // Mon, Wed, Fri
            HabitDef(3, "Old habit", ALL_DAYS, today - 40, today - 2), // archived 2 days ago
            HabitDef(4, "Future", ALL_DAYS, today + 5, null),
        )
        val tasks = listOf(
            TaskLite(TaskKind.IMPORTANT, "Report", true), TaskLite(TaskKind.IMPORTANT, "Call", false),
            TaskLite(TaskKind.NORMAL, "Read", true), TaskLite(TaskKind.EXTRA, "Explore", true),
        )
        val d = SummaryBuilder.build(today, habits, setOf(1L, 2L), tasks, listOf("idea a", "idea b"), 90 * 60_000L, "good day")
        val s = d.summary
        check("monday: water+gym due, archived/future excluded", s.habitsTotal == 2 && s.habitsDone == 2)
        check("task lanes counted", s.importantDone == 1 && s.importantTotal == 2 && s.normalDone == 1 && s.extraTotal == 1)
        check("ideas + tracked time", s.ideasCaptured == 2 && s.trackedMs == 5_400_000L)
        check("not complete while an important task is open", !s.dayComplete)
        check("weighted score", Math.abs(s.score - (0.40f * 1f + 0.35f * 0.5f + 0.25f * 1f)) < 1e-5)
        check("note carried", d.note == "good day")
        val tuesday = SummaryBuilder.build(today + 1, habits, setOf(1L), emptyList(), emptyList(), 0, "")
        check("gym not due Tuesday", tuesday.summary.habitsTotal == 1 && tuesday.habits.single().title == "Water")
        val past = SummaryBuilder.build(today - 5, habits, emptySet(), emptyList(), emptyList(), 0, "")
        check("archived habit still counted before archive day", past.habits.any { it.title == "Old habit" })
        check("empty summary has no data", DaySummary.empty(today).hasData.not())
        val all = SummaryBuilder.build(today, listOf(habits[0]), setOf(1L), listOf(TaskLite(TaskKind.IMPORTANT, "x", true)), emptyList(), 0, "")
        check("day complete when all core done", all.summary.dayComplete && all.summary.score == 1f)

        // Streaks.longest
        val done = setOf(today - 10, today - 9, today - 8, today - 6, today - 5)
        check("longest run is 3", Streaks.longest(ALL_DAYS, today - 20, done, today) == 3)
        check("open today does not break run", Streaks.longest(ALL_DAYS, today - 20, setOf(today - 2, today - 1), today) == 2)
        check("weekend gap ignored for weekday habit", Streaks.longest(0b0011111, today - 20, setOf(today - 4, today - 3, today), today) == 3)

        // Habit stats
        val water = HabitDef(1, "Water", ALL_DAYS, today - 40, null)
        val doneDays = (today - 9..today - 1).toSet() + today // last 10 days incl. today
        val st = HabitStatsCalc.compute(water, doneDays, today, 30)
        check("10 of 30 days = 33%", st.dueDays == 30 && st.doneDays == 10 && st.percent == 33)
        check("current streak 10, best 10", st.current == 10 && st.best == 10)
        val open = HabitStatsCalc.compute(water, doneDays - today, today, 30)
        check("open today not counted as missed", open.dueDays == 29 && open.doneDays == 9)
        val dots = HabitStatsCalc.dots(water, doneDays - today, today, 28)
        check("28 dots, today open, yesterday done", dots.size == 28 && dots.last() == DotState.OPEN && dots[26] == DotState.DONE && dots[0] == DotState.MISSED)
        val gym = HabitDef(2, "Gym", 0b0010101, today - 40, null)
        check("not-due days marked", HabitStatsCalc.dots(gym, emptySet(), today, 7).count { it == DotState.NOT_DUE } == 4)

        // Calendar
        val wed = day(2026, 11, 18)
        check("week starts Monday", CalendarMath.startOfWeek(wed, DayOfWeek.MONDAY.value) == today)
        check("week starts Sunday", CalendarMath.startOfWeek(wed, DayOfWeek.SUNDAY.value) == today - 1)
        check("week starts Saturday", CalendarMath.startOfWeek(today, DayOfWeek.SATURDAY.value) == today - 2)
        check("sunday belongs to previous Monday-week", CalendarMath.startOfWeek(day(2026, 11, 22), 1) == today)
        val nov = CalendarMath.monthGrid(today, DayOfWeek.MONDAY.value)   // Nov 1 2026 is a Sunday
        check("grid multiple of 7, 30 days, 6 leading blanks (Mon start)", nov.size % 7 == 0 && nov.count { it != null } == 30 && nov.indexOfFirst { it != null } == 6)
        val novSun = CalendarMath.monthGrid(today, DayOfWeek.SUNDAY.value)
        check("Sunday start has no leading blanks", novSun.indexOfFirst { it != null } == 0)
        check("grid ordered", nov.filterNotNull() == nov.filterNotNull().sorted())
        check("month bounds", CalendarMath.monthStart(today) == day(2026, 11, 1) && CalendarMath.monthEnd(today) == day(2026, 11, 30))
        check("shift month across year", CalendarMath.shiftMonth(day(2026, 12, 15), 1) == day(2027, 1, 1))
        check("weekday labels rotate", CalendarMath.weekdayLabels(7, Locale.ENGLISH) == listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"))
        check("month title", CalendarMath.monthTitle(today, Locale.ENGLISH) == "November 2026")

        // Review reminder
        val z = ZoneId.of("Asia/Kathmandu")
        val plan = RemindersPlan.build(today, z, emptyList(), emptyList(), emptyList(), emptyMap(), 5, setOf(ReminderCategory.REVIEW), reviewMinute = 21 * 60 + 30)
        check("review reminder today + tomorrow", plan.map { it.key } == listOf("R:0:$today", "R:0:${today + 1}") && plan.first().atMillis == RemindersPlan.atMillis(today, 21 * 60 + 30, z))
        check("review off by default category set", RemindersPlan.build(today, z, emptyList(), emptyList(), emptyList(), emptyMap(), 5, emptySet()).isEmpty())
    }
}
