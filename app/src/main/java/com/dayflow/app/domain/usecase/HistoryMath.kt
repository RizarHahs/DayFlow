package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.time.isScheduled

object SummaryBuilder {
    /**
     * Builds the day's detail from plain inputs. Habits count only if they were due that day;
     * archived habits stop counting from their archive day onward but keep all earlier days.
     */
    fun build(
        day: Int,
        habits: List<HabitDef>,
        doneHabitIds: Set<Long>,
        tasks: List<TaskLite>,
        ideaTexts: List<String>,
        trackedMs: Long,
        note: String,
        weights: ScoreWeights = ScoreWeights(),
    ): DayDetail {
        val due = habits.filter { HabitSchedule.isDue(it.repeatMask, it.createdDay, it.archivedDay, day) }
        val habitItems = due.map { DetailItem(it.title, it.id in doneHabitIds) }
        fun lane(kind: TaskKind) = tasks.filter { it.kind == kind }.map { DetailItem(it.title, it.done) }
        val imp = lane(TaskKind.IMPORTANT)
        val nor = lane(TaskKind.NORMAL)
        val ext = lane(TaskKind.EXTRA)
        val f = CategoryProgress(habitItems.count { it.done }, habitItems.size)
        val i = CategoryProgress(imp.count { it.done }, imp.size)
        val n = CategoryProgress(nor.count { it.done }, nor.size)
        val summary = DaySummary(
            day = day,
            habitsDone = f.done, habitsTotal = f.total,
            importantDone = i.done, importantTotal = i.total,
            normalDone = n.done, normalTotal = n.total,
            extraDone = ext.count { it.done }, extraTotal = ext.size,
            score = DayScoring.weighted(f, i, n, weights),
            trackedMs = trackedMs,
            ideasCaptured = ideaTexts.size,
            dayComplete = DayScoring.dayComplete(f, i, n),
        )
        return DayDetail(summary, habitItems, imp, nor, ext, ideaTexts, note)
    }
}

object HabitStatsCalc {
    /** Consistency over the last [window] days (only days the habit was due count), plus streaks. */
    fun compute(h: HabitDef, doneDays: Set<Int>, today: Int, window: Int = 30): HabitStats {
        var due = 0
        var done = 0
        for (d in (today - window + 1)..today) {
            if (!HabitSchedule.isDue(h.repeatMask, h.createdDay, h.archivedDay, d)) continue
            // today still open is not counted against the habit
            if (d == today && d !in doneDays) continue
            due++
            if (d in doneDays) done++
        }
        val end = h.archivedDay?.let { it - 1 } ?: today
        return HabitStats(
            dueDays = due, doneDays = done,
            current = Streaks.current(h.repeatMask, h.createdDay, doneDays, minOf(today, end)),
            best = Streaks.longest(h.repeatMask, h.createdDay, doneDays, today),
        )
    }

    fun dots(h: HabitDef, doneDays: Set<Int>, today: Int, count: Int = 28): List<DotState> =
        ((today - count + 1)..today).map { d ->
            when {
                !HabitSchedule.isDue(h.repeatMask, h.createdDay, h.archivedDay, d) -> DotState.NOT_DUE
                d in doneDays -> DotState.DONE
                d == today -> DotState.OPEN
                else -> DotState.MISSED
            }
        }
}
