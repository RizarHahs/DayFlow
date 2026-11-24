package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.time.isScheduled

object HabitSchedule {
    /** Due on [day] if scheduled that weekday, already created, and not yet archived. */
    fun isDue(mask: Int, createdDay: Int, archivedDay: Int?, day: Int): Boolean =
        day >= createdDay && (archivedDay == null || day < archivedDay) && isScheduled(mask, day)
}

object Streaks {
    /**
     * Consecutive scheduled days completed, ending today.
     * Today never breaks a streak while it is still open; unscheduled days are skipped, not counted.
     */
    fun current(mask: Int, createdDay: Int, doneDays: Set<Int>, today: Int): Int {
        var count = 0
        if (isScheduled(mask, today) && today in doneDays) count++
        val floor = minOf(createdDay, doneDays.minOrNull() ?: createdDay)
        var d = today - 1
        var steps = 0
        while (d >= floor && steps < 3660) {
            if (isScheduled(mask, d)) {
                if (d in doneDays) count++ else break
            }
            d--
            steps++
        }
        return count
    }

    /** Longest run of consecutive scheduled days completed, across the habit's whole history. */
    fun longest(mask: Int, createdDay: Int, doneDays: Set<Int>, today: Int): Int {
        val start = minOf(createdDay, doneDays.minOrNull() ?: createdDay)
        var best = 0
        var run = 0
        for (d in start..today) {
            if (!isScheduled(mask, d)) continue
            if (d in doneDays) { run++; if (run > best) best = run }
            else if (d != today) run = 0      // an open today does not break the run
        }
        return best
    }
}
