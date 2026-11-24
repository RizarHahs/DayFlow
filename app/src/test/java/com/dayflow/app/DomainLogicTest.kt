package com.dayflow.app

import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.time.*
import com.dayflow.app.domain.usecase.*
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM checks of the Phase 1 rules: streaks, scoring, roadmap status, conflicts, tags. */
class DomainLogicTest {
    private fun check(name: String, ok: Boolean) = assertTrue(name, ok)

    @Test
    fun domainRules() {

        val monday = java.time.LocalDate.of(2026, 11, 9).toEpochDay().toInt() // Mon
        check("weekdayIndex Monday=0", weekdayIndex(monday) == 0)
        check("weekdayIndex Sunday=6", weekdayIndex(monday + 6) == 6)
        check("maskLabel all", maskLabel(ALL_DAYS) == "Every day")
        check("maskLabel weekdays", maskLabel(0b0011111) == "Weekdays")
        check("maskLabel custom", maskLabel(0b0000101) == "Mon Wed")

    // Streaks: done Mon..Wed, today = Wed done -> 3
        check("streak 3", Streaks.current(ALL_DAYS, monday, setOf(monday, monday + 1, monday + 2), monday + 2) == 3)
    // today open does not break streak
        check("streak today open", Streaks.current(ALL_DAYS, monday, setOf(monday, monday + 1), monday + 2) == 2)
    // missed yesterday breaks
        check("streak broken", Streaks.current(ALL_DAYS, monday, setOf(monday, monday + 2), monday + 2) == 1)
    // weekday-only habit skips weekend
        val fri = monday + 4; val nextMon = monday + 7
        check("streak skips weekend", Streaks.current(0b0011111, monday, setOf(fri - 1, fri, nextMon), nextMon) == 3)
        check("habit due", HabitSchedule.isDue(ALL_DAYS, monday, null, monday + 1))
        check("habit not due before created", !HabitSchedule.isDue(ALL_DAYS, monday + 3, null, monday))
        check("habit not due after archive", !HabitSchedule.isDue(ALL_DAYS, monday, monday + 2, monday + 2))

    // Scoring
        val full = CategoryProgress(2, 2); val half = CategoryProgress(1, 2); val none = CategoryProgress(0, 0)
        check("score all done = 1", DayScoring.weighted(full, full, full) == 1f)
        check("score renormalises empty", DayScoring.weighted(half, none, none) == 0.5f)
        check("score weighted", Math.abs(DayScoring.weighted(full, half, CategoryProgress(0, 2)) - (0.40f + 0.175f)) < 1e-5)
        check("day complete", DayScoring.dayComplete(full, full, none))
        check("day not complete", !DayScoring.dayComplete(full, half, full))
        check("empty day not complete", !DayScoring.dayComplete(none, none, none))

    // Roadmap slots
        val items = listOf(
        RoadmapItem(1, "Wake", 6 * 60 + 30, 7 * 60, ALL_DAYS, null, null),
        RoadmapItem(2, "Study", 9 * 60, 11 * 60, ALL_DAYS, null, null),
        RoadmapItem(3, "Gym", 10 * 60, 11 * 60 + 30, ALL_DAYS, 99, null), // overlaps Study, linked habit 99
        RoadmapItem(4, "WeekendOnly", 12 * 60, 13 * 60, 0b1100000, null, null),
    )
        val slots = RoadmapSlots.build(monday, 9 * 60 + 30, items, emptyList(), emptySet(), emptySet())
        check("weekend item hidden on Monday", slots.none { it.itemId == 4L })
        check("wake missed", slots.first { it.itemId == 1L }.status == SlotStatus.MISSED)
        check("study current", slots.first { it.itemId == 2L }.status == SlotStatus.CURRENT)
        check("gym upcoming", slots.first { it.itemId == 3L }.status == SlotStatus.UPCOMING)
        check("overlap flagged both", slots.first { it.itemId == 2L }.conflict && slots.first { it.itemId == 3L }.conflict)
        check("wake not conflict", !slots.first { it.itemId == 1L }.conflict)
        val withDone = RoadmapSlots.build(monday, 9 * 60 + 30, items, emptyList(), setOf(99L), emptySet())
        check("linked habit marks slot done", withDone.first { it.itemId == 3L }.status == SlotStatus.DONE)
        val skip = RoadmapSlots.build(monday, 9 * 60 + 30, items, listOf(RoadmapDayLog(2, monday, DayLogStatus.SKIPPED, null, null)), emptySet(), emptySet())
        check("skipped slot", skip.first { it.itemId == 2L }.status == SlotStatus.SKIPPED)
        check("skipped removes conflict", !skip.first { it.itemId == 3L }.conflict)
        val moved = RoadmapSlots.build(monday, 9 * 60 + 30, items, listOf(RoadmapDayLog(1, monday, DayLogStatus.RESCHEDULED, 20 * 60, 21 * 60)), emptySet(), emptySet())
        val m = moved.first { it.itemId == 1L }
        check("reschedule overrides times + upcoming", m.startMinute == 20 * 60 && m.status == SlotStatus.UPCOMING && m.rescheduled)
        val past = RoadmapSlots.build(monday - 1, 1440, items, emptyList(), emptySet(), emptySet())
        check("past day all missed", past.all { it.status == SlotStatus.MISSED })
        val future = RoadmapSlots.build(monday + 1, -1, items, emptyList(), emptySet(), emptySet())
        check("future day all upcoming", future.all { it.status == SlotStatus.UPCOMING })

    // Conflicts for recurring drafts
        val d = RoadmapDraft(id = 0, title = "X", startMinute = 9 * 60 + 30, endMinute = 10 * 60 + 30, repeatMask = ALL_DAYS)
        check("recurring conflict detects 2", Conflicts.forRecurring(d, items).map { it.id }.toSet() == setOf(2L, 3L))
        val wk = d.copy(repeatMask = 0b1100000, startMinute = 12 * 60, endMinute = 12 * 60 + 30)
        check("recurring conflict weekday-aware", Conflicts.forRecurring(wk, items).map { it.id } == listOf(4L))
        check("touching intervals do not overlap", !Conflicts.overlaps(9 * 60, 10 * 60, 10 * 60, 11 * 60))
        check("editing self excluded", Conflicts.forRecurring(RoadmapDraft(id = 2, startMinute = 9 * 60, endMinute = 11 * 60), items).none { it.id == 2L })

    // Tags
        check("tags normalised", normalizeTags(listOf("#Design", " design ", "Big Idea", "")) == listOf("design", "big-idea"))
    // Defaults
        check("rollover defaults", RolloverPolicy.defaultFor(TaskKind.IMPORTANT) == RolloverPolicy.ASK && RolloverPolicy.defaultFor(TaskKind.NORMAL) == RolloverPolicy.CARRY)

    }
}
