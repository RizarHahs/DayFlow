package com.dayflow.app.platform.alarm

import com.dayflow.app.data.local.AlarmDao
import com.dayflow.app.data.local.AlarmEntity
import com.dayflow.app.data.repo.toDomain
import com.dayflow.app.domain.model.Alarm
import com.dayflow.app.domain.model.AlarmDraft
import com.dayflow.app.domain.repo.AlarmController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmControllerImpl @Inject constructor(
    private val dao: AlarmDao,
    private val scheduler: AlarmScheduler,
) : AlarmController {

    override fun observeAlarms(): Flow<List<Alarm>> = dao.observeAll().map { l -> l.map { it.toDomain() } }

    override suspend fun get(id: Long): Alarm? = dao.get(id)?.toDomain()

    override suspend fun save(draft: AlarmDraft): Long {
        val existing = if (draft.id != 0L) dao.get(draft.id) else null
        val entity = (existing ?: AlarmEntity(
            minute = draft.minute, repeatMask = draft.repeatMask, label = "", enabled = draft.enabled,
            kind = draft.kind, soundUri = null, vibrate = true, snoozeMin = 9,
            linkedHabitId = null, linkedRoadmapId = null, nextTriggerAt = null,
        )).copy(
            minute = draft.minute, repeatMask = draft.repeatMask, label = draft.label.trim(),
            enabled = draft.enabled, kind = draft.kind, soundUri = draft.soundUri, vibrate = draft.vibrate,
            snoozeMin = draft.snoozeMin, linkedHabitId = draft.linkedHabitId, linkedRoadmapId = draft.linkedRoadmapId,
        )
        val id = if (existing == null) dao.insert(entity) else { dao.update(entity); entity.id }
        scheduler.schedule(entity.copy(id = id))
        return id
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        val a = dao.get(id) ?: return
        val updated = a.copy(enabled = enabled)
        dao.update(updated)
        scheduler.schedule(updated)
    }

    override suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.delete(id)
    }

    override fun scheduleTest(delayMs: Long): Boolean = scheduler.scheduleTest(delayMs)
}
