package com.dayflow.app.data.repo

import androidx.room.withTransaction
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.local.SubtaskEntity
import com.dayflow.app.data.local.TaskEntity
import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.time.DayClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val db: DayflowDatabase,
    private val clock: DayClock,
) : TaskRepository {
    private val dao = db.taskDao()
    private val subDao = db.subtaskDao()

    private fun List<TaskEntity>.withSubs(subs: List<SubtaskEntity>, today: Int): List<TaskItem> {
        val by = subs.groupBy { it.taskId }
        return map { t -> t.toDomain(by[t.id].orEmpty().map { it.toDomain() }, today) }
    }

    override fun observeToday(day: Int): Flow<List<TaskItem>> =
        combine(dao.observeToday(day), subDao.observeAll()) { tasks, subs -> tasks.withSubs(subs, day) }

    override fun observeUpcoming(day: Int): Flow<List<TaskItem>> =
        combine(dao.observeUpcoming(day), subDao.observeAll()) { tasks, subs -> tasks.withSubs(subs, day) }

    override suspend fun getItem(id: Long): TaskItem? =
        dao.get(id)?.let { listOf(it).withSubs(subDao.forTask(id), clock.today()).first() }

    override suspend fun getDraft(id: Long): TaskDraft? {
        val t = dao.get(id) ?: return null
        return TaskDraft(
            id = t.id, kind = t.kind, title = t.title, description = t.description, priority = t.priority,
            plannedDay = t.plannedDay, dueDay = t.dueDay, startMinute = t.startMinute, endMinute = t.endMinute,
            estimateMin = t.estimateMin, reminderMinute = t.reminderMinute, timerSeconds = t.timerSeconds,
            rolloverPolicy = t.rolloverPolicy,
            subtasks = subDao.forTask(id).map { SubtaskDraft(it.id, it.title, it.done) },
            sourceIdeaId = t.sourceIdeaId,
        )
    }

    override suspend fun save(draft: TaskDraft): Long = db.withTransaction {
        val existing = if (draft.id != 0L) dao.get(draft.id) else null
        val priority = if (draft.kind == TaskKind.IMPORTANT) draft.priority ?: Priority.MEDIUM else null
        val id = if (existing == null) {
            dao.insert(
                TaskEntity(
                    kind = draft.kind, title = draft.title.trim(), description = draft.description.trim(),
                    priority = priority, plannedDay = draft.plannedDay, dueDay = draft.dueDay,
                    startMinute = draft.startMinute, endMinute = draft.endMinute,
                    estimateMin = draft.estimateMin, reminderMinute = draft.reminderMinute,
                    timerSeconds = draft.timerSeconds, status = TaskStatus.OPEN,
                    completedAt = null, completedDay = null,
                    rolloverPolicy = draft.rolloverPolicy, rolloverCount = 0,
                    sortOrder = dao.maxOrder(draft.kind) + 1, sourceIdeaId = draft.sourceIdeaId,
                    promotedFromKind = null, createdAt = System.currentTimeMillis(),
                )
            )
        } else {
            val kindChanged = existing.kind != draft.kind
            dao.update(
                existing.copy(
                    kind = draft.kind, title = draft.title.trim(), description = draft.description.trim(),
                    priority = priority, plannedDay = draft.plannedDay, dueDay = draft.dueDay,
                    startMinute = draft.startMinute, endMinute = draft.endMinute,
                    estimateMin = draft.estimateMin, reminderMinute = draft.reminderMinute,
                    timerSeconds = draft.timerSeconds, rolloverPolicy = draft.rolloverPolicy,
                    sortOrder = if (kindChanged) dao.maxOrder(draft.kind) + 1 else existing.sortOrder,
                    promotedFromKind = if (kindChanged) existing.kind else existing.promotedFromKind,
                )
            )
            existing.id
        }
        subDao.deleteForTask(id)
        subDao.insertAll(
            draft.subtasks.filter { it.title.isNotBlank() }.mapIndexed { i, s ->
                SubtaskEntity(taskId = id, title = s.title.trim(), done = s.done, sortOrder = i)
            }
        )
        id
    }

    override suspend fun toggleDone(id: Long, day: Int) {
        val t = dao.get(id) ?: return
        val nowDone = t.status != TaskStatus.DONE
        dao.update(
            t.copy(
                status = if (nowDone) TaskStatus.DONE else TaskStatus.OPEN,
                completedAt = if (nowDone) System.currentTimeMillis() else null,
                completedDay = if (nowDone) day else null,
            )
        )
    }

    override suspend fun toggleSubtask(subtaskId: Long) = subDao.toggle(subtaskId)

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun reschedule(id: Long, day: Int, countRollover: Boolean) {
        val t = dao.get(id) ?: return
        dao.update(t.copy(plannedDay = day, rolloverCount = t.rolloverCount + if (countRollover) 1 else 0))
    }

    override suspend fun drop(id: Long) {
        val t = dao.get(id) ?: return
        dao.update(t.copy(status = TaskStatus.DROPPED))
    }

    override suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { i, id -> dao.setOrder(id, i + 1) }
    }

    override suspend fun changeKind(id: Long, kind: TaskKind) {
        val t = dao.get(id) ?: return
        if (t.kind == kind) return
        dao.update(
            t.copy(
                kind = kind,
                priority = if (kind == TaskKind.IMPORTANT) t.priority ?: Priority.MEDIUM else null,
                promotedFromKind = t.kind,
                sortOrder = dao.maxOrder(kind) + 1,
            )
        )
    }

    override suspend fun openBefore(day: Int): List<TaskItem> =
        dao.openBefore(day).map { it.toDomain(emptyList(), day) }
}
