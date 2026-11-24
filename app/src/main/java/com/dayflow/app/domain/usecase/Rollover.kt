package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.RolloverPolicy
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.repo.TransactionRunner
import javax.inject.Inject

/**
 * New-day processing for unfinished tasks. Idempotent: carried tasks move to today, so a second
 * run finds nothing. Nothing is ever deleted: DROP only flips status, ASK leaves the task visible
 * as overdue until the user decides. Habit history is never touched (it is keyed by day).
 */
class RolloverUseCase @Inject constructor(
    private val tasks: TaskRepository,
    private val tx: TransactionRunner,
) {
    suspend operator fun invoke(today: Int) {
        val stale = tasks.openBefore(today)
        if (stale.isEmpty()) return
        tx.run {
            stale.forEach { t ->
                when (t.rolloverPolicy) {
                    RolloverPolicy.CARRY -> tasks.reschedule(t.id, today, countRollover = true)
                    RolloverPolicy.DROP -> tasks.drop(t.id)
                    RolloverPolicy.ASK -> Unit
                }
            }
        }
    }
}
