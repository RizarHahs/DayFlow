package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.IdeaRepository
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.repo.TransactionRunner
import com.dayflow.app.domain.time.DayClock
import javax.inject.Inject

/** Idea -> anything, Extra/Normal/Important <-> each other, Task -> Habit. Information is carried over. */
class ConvertUseCase @Inject constructor(
    private val tx: TransactionRunner,
    private val ideas: IdeaRepository,
    private val habits: HabitRepository,
    private val tasks: TaskRepository,
    private val clock: DayClock,
) {
    suspend fun ideaTo(ideaId: Long, target: ItemKind) {
        val idea = ideas.get(ideaId) ?: return
        if (target == ItemKind.IDEA) return
        val lines = idea.text.lines()
        val title = lines.first().trim().take(80).ifBlank { "Untitled idea" }
        val rest = (lines.drop(1).joinToString("\n").trim() + "\n" + idea.description).trim()
        tx.run {
            when (target) {
                ItemKind.HABIT -> habits.save(HabitDraft(title = title, sourceIdeaId = ideaId))
                else -> {
                    val kind = when (target) {
                        ItemKind.IMPORTANT -> TaskKind.IMPORTANT
                        ItemKind.EXTRA -> TaskKind.EXTRA
                        else -> TaskKind.NORMAL
                    }
                    tasks.save(
                        TaskDraft(
                            kind = kind, title = title, description = rest,
                            priority = if (kind == TaskKind.IMPORTANT) Priority.MEDIUM else null,
                            plannedDay = clock.today(), sourceIdeaId = ideaId,
                        )
                    )
                }
            }
            ideas.markConverted(ideaId, target)
        }
    }

    /** Changes the lane of a task (promote/demote). Everything else on the task is preserved. */
    suspend fun taskTo(taskId: Long, target: TaskKind) = tasks.changeKind(taskId, target)

    /** Normal target -> daily habit. The original task is dropped (kept in the database, hidden). */
    suspend fun taskToHabit(taskId: Long) {
        val t = tasks.getItem(taskId) ?: return
        tx.run {
            habits.save(
                HabitDraft(
                    title = t.title,
                    period = periodOf(t.startMinute),
                    minute = t.startMinute,
                    reminderMinute = t.reminderMinute,
                    timerSeconds = t.timerSeconds ?: t.estimateMin?.let { it * 60 },
                )
            )
            tasks.drop(taskId)
        }
    }

    private fun periodOf(minute: Int?) = when {
        minute == null -> Period.MORNING
        minute < 12 * 60 -> Period.MORNING
        minute < 17 * 60 -> Period.AFTERNOON
        else -> Period.EVENING
    }
}
