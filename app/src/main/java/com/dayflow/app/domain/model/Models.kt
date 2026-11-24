package com.dayflow.app.domain.model

enum class Period(val label: String) { MORNING("Morning"), AFTERNOON("Afternoon"), EVENING("Evening") }

enum class Priority(val label: String) { CRITICAL("Critical"), HIGH("High"), MEDIUM("Medium") }

/** What a quick-add / conversion creates. */
enum class ItemKind { HABIT, IMPORTANT, NORMAL, EXTRA, IDEA }

/** The three task flavours share one table (Phase 1 decision) so promotion is a column update. */
enum class TaskKind { IMPORTANT, NORMAL, EXTRA;
    fun toItemKind() = when (this) { IMPORTANT -> ItemKind.IMPORTANT; NORMAL -> ItemKind.NORMAL; EXTRA -> ItemKind.EXTRA }
}

enum class TaskStatus { OPEN, DONE, DROPPED }

enum class RolloverPolicy(val label: String) {
    CARRY("Carry to the next day"),
    ASK("Ask me what to do"),
    DROP("Let it go");

    companion object {
        fun defaultFor(kind: TaskKind) = when (kind) {
            TaskKind.IMPORTANT -> ASK   // important things deserve a conscious decision
            TaskKind.NORMAL -> CARRY
            TaskKind.EXTRA -> CARRY
        }
    }
}

enum class SlotStatus { UPCOMING, CURRENT, DONE, MISSED, SKIPPED }
enum class DayLogStatus { DONE, SKIPPED, RESCHEDULED }

data class NewItem(
    val kind: ItemKind,
    val title: String,
    val priority: Priority = Priority.HIGH,
    val period: Period = Period.MORNING,
)

// ---------- Read models (what the UI sees) ----------

/** [minute] = preferred time of day, minutes since midnight. [repeatMask] bit0 = Monday ... bit6 = Sunday. */
data class Habit(
    val id: Long,
    val title: String,
    val period: Period,
    val minute: Int?,
    val repeatMask: Int,
    val reminderMinute: Int?,
    val timerSeconds: Int?,
    val createdDay: Int,
    val done: Boolean,
    val streak: Int,
    val dueToday: Boolean,
)

data class Subtask(val id: Long, val title: String, val done: Boolean)

data class TaskItem(
    val id: Long,
    val kind: TaskKind,
    val title: String,
    val description: String = "",
    val done: Boolean,
    val priority: Priority? = null,
    val plannedDay: Int = 0,
    val dueDay: Int? = null,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val estimateMin: Int? = null,
    val reminderMinute: Int? = null,
    val timerSeconds: Int? = null,
    val rolloverPolicy: RolloverPolicy = RolloverPolicy.CARRY,
    val rolloverCount: Int = 0,
    val subtasks: List<Subtask> = emptyList(),
    val overdue: Boolean = false,
)

data class Idea(
    val id: Long,
    val text: String,
    val description: String,
    val pinned: Boolean,
    val archived: Boolean,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val convertedTo: ItemKind?,
)

data class RoadmapEntry(val id: Long, val title: String, val startMinute: Int, val endMinute: Int)

data class RoadmapItem(
    val id: Long,
    val title: String,
    val startMinute: Int,
    val endMinute: Int,
    val repeatMask: Int,
    val linkedHabitId: Long?,
    val linkedTaskId: Long?,
)

data class RoadmapDayLog(
    val itemId: Long,
    val day: Int,
    val status: DayLogStatus,
    val overrideStart: Int?,
    val overrideEnd: Int?,
)

/** One activity on one specific day, with effective times and a derived status. */
data class RoadmapSlot(
    val itemId: Long,
    val title: String,
    val startMinute: Int,
    val endMinute: Int,
    val status: SlotStatus,
    val rescheduled: Boolean,
    val conflict: Boolean,
    val linkedHabitId: Long?,
    val linkedTaskId: Long?,
)

// ---------- Drafts (what editors submit) ----------

data class HabitDraft(
    val id: Long = 0,
    val title: String = "",
    val period: Period = Period.MORNING,
    val minute: Int? = null,
    val repeatMask: Int = ALL_DAYS,
    val reminderMinute: Int? = null,
    val timerSeconds: Int? = null,
    val sourceIdeaId: Long? = null,
)

data class SubtaskDraft(val id: Long = 0, val title: String, val done: Boolean = false)

data class TaskDraft(
    val id: Long = 0,
    val kind: TaskKind = TaskKind.NORMAL,
    val title: String = "",
    val description: String = "",
    val priority: Priority? = null,
    val plannedDay: Int = 0,
    val dueDay: Int? = null,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val estimateMin: Int? = null,
    val reminderMinute: Int? = null,
    val timerSeconds: Int? = null,
    val rolloverPolicy: RolloverPolicy = RolloverPolicy.defaultFor(kind),
    val subtasks: List<SubtaskDraft> = emptyList(),
    val sourceIdeaId: Long? = null,
)

data class IdeaDraft(
    val id: Long = 0,
    val text: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
)

data class RoadmapDraft(
    val id: Long = 0,
    val title: String = "",
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 10 * 60,
    val repeatMask: Int = ALL_DAYS,
    val linkedHabitId: Long? = null,
    val linkedTaskId: Long? = null,
)

// ---------- Scoring ----------

data class CategoryProgress(val done: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
    val isEmpty: Boolean get() = total == 0
}

data class ScoreWeights(
    val foundation: Float = 0.40f,
    val important: Float = 0.35f,
    val normal: Float = 0.25f,
)

const val ALL_DAYS = 127
