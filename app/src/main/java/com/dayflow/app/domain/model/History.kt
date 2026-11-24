package com.dayflow.app.domain.model

/** Snapshot of one day. Stored for past days so editing a habit later never rewrites history. */
data class DaySummary(
    val day: Int,
    val habitsDone: Int, val habitsTotal: Int,
    val importantDone: Int, val importantTotal: Int,
    val normalDone: Int, val normalTotal: Int,
    val extraDone: Int, val extraTotal: Int,
    val score: Float,
    val trackedMs: Long,
    val ideasCaptured: Int,
    val dayComplete: Boolean,
) {
    /** True when there is anything at all to show for the day. */
    val hasData: Boolean get() = habitsTotal + importantTotal + normalTotal + extraTotal > 0 || trackedMs > 0 || ideasCaptured > 0

    companion object {
        fun empty(day: Int) = DaySummary(day, 0, 0, 0, 0, 0, 0, 0, 0, 0f, 0L, 0, false)
    }
}

data class DetailItem(val title: String, val done: Boolean)

data class DayDetail(
    val summary: DaySummary,
    val habits: List<DetailItem>,
    val important: List<DetailItem>,
    val normal: List<DetailItem>,
    val extra: List<DetailItem>,
    val ideas: List<String>,
    val note: String,
)

/** Inputs for building a summary; independent of Room so the maths is unit-testable. */
data class HabitDef(val id: Long, val title: String, val repeatMask: Int, val createdDay: Int, val archivedDay: Int?)
data class TaskLite(val kind: TaskKind, val title: String, val done: Boolean)

enum class DotState { DONE, OPEN, MISSED, NOT_DUE }

data class HabitStats(val dueDays: Int, val doneDays: Int, val current: Int, val best: Int) {
    val percent: Int get() = if (dueDays == 0) 0 else Math.round(doneDays * 100f / dueDays)
}

data class HabitHistory(
    val id: Long,
    val title: String,
    val archived: Boolean,
    val stats: HabitStats,
    val dots: List<DotState>,   // last 28 days, oldest first
)

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppearanceSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val weekStart: Int = 1,          // java.time.DayOfWeek value: 1 = Monday ... 7 = Sunday
    val use24h: Boolean = true,
    val defaultTimerMin: Int = 25,
)
