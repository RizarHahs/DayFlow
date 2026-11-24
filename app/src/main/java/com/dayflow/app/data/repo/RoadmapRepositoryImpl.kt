package com.dayflow.app.data.repo

import com.dayflow.app.data.local.RoadmapDao
import com.dayflow.app.data.local.RoadmapDayEntity
import com.dayflow.app.data.local.RoadmapItemEntity
import com.dayflow.app.domain.model.DayLogStatus
import com.dayflow.app.domain.model.RoadmapDayLog
import com.dayflow.app.domain.model.RoadmapDraft
import com.dayflow.app.domain.model.RoadmapItem
import com.dayflow.app.domain.repo.RoadmapRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoadmapRepositoryImpl @Inject constructor(private val dao: RoadmapDao) : RoadmapRepository {

    override fun observeItems(): Flow<List<RoadmapItem>> = dao.observeItems().map { l -> l.map { it.toDomain() } }
    override fun observeDayLogs(day: Int): Flow<List<RoadmapDayLog>> = dao.observeDay(day).map { l -> l.map { it.toDomain() } }
    override suspend fun getItem(id: Long): RoadmapItem? = dao.getItem(id)?.toDomain()
    override suspend fun activeItems(): List<RoadmapItem> = dao.activeItems().map { it.toDomain() }

    override suspend fun save(draft: RoadmapDraft): Long {
        val existing = if (draft.id != 0L) dao.getItem(draft.id) else null
        return if (existing == null) {
            dao.insertItem(
                RoadmapItemEntity(
                    title = draft.title.trim(), startMinute = draft.startMinute, endMinute = draft.endMinute,
                    repeatMask = draft.repeatMask, linkedHabitId = draft.linkedHabitId,
                    linkedTaskId = draft.linkedTaskId, sortOrder = dao.maxOrder() + 1, active = true,
                )
            )
        } else {
            dao.updateItem(
                existing.copy(
                    title = draft.title.trim(), startMinute = draft.startMinute, endMinute = draft.endMinute,
                    repeatMask = draft.repeatMask, linkedHabitId = draft.linkedHabitId, linkedTaskId = draft.linkedTaskId,
                )
            )
            existing.id
        }
    }

    override suspend fun delete(id: Long) = dao.deactivate(id)

    private suspend fun upsert(itemId: Long, day: Int, status: DayLogStatus, completedAt: Long? = null) {
        val cur = dao.getDay(itemId, day)
        dao.upsertDay(
            RoadmapDayEntity(itemId, day, status, cur?.overrideStart, cur?.overrideEnd, completedAt)
        )
    }

    override suspend fun setDone(itemId: Long, day: Int, done: Boolean) {
        val cur = dao.getDay(itemId, day)
        when {
            done -> upsert(itemId, day, DayLogStatus.DONE, System.currentTimeMillis())
            cur?.overrideStart != null -> upsert(itemId, day, DayLogStatus.RESCHEDULED)
            else -> dao.clearDay(itemId, day)
        }
    }

    override suspend fun skip(itemId: Long, day: Int) = upsert(itemId, day, DayLogStatus.SKIPPED)

    override suspend fun reschedule(itemId: Long, day: Int, start: Int, end: Int) {
        val cur = dao.getDay(itemId, day)
        val keep = cur?.status == DayLogStatus.DONE
        dao.upsertDay(
            RoadmapDayEntity(
                itemId, day, if (keep) DayLogStatus.DONE else DayLogStatus.RESCHEDULED,
                start, end, cur?.completedAt,
            )
        )
    }

    override suspend fun clearDay(itemId: Long, day: Int) = dao.clearDay(itemId, day)
}
