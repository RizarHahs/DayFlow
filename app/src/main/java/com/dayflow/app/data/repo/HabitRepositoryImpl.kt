package com.dayflow.app.data.repo

import com.dayflow.app.data.local.HabitDao
import com.dayflow.app.data.local.HabitEntity
import com.dayflow.app.data.local.HabitLogEntity
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitDraft
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.usecase.HabitSchedule
import com.dayflow.app.domain.usecase.Streaks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val dao: HabitDao,
    private val clock: DayClock,
) : HabitRepository {

    override fun observeHabits(day: Int): Flow<List<Habit>> =
        combine(dao.observeActive(), dao.observeLogs()) { habits, logs ->
            val byHabit = logs.groupBy { it.habitId }
            habits.map { h ->
                val doneDays = byHabit[h.id]?.map { it.day }?.toSet() ?: emptySet()
                Habit(
                    id = h.id, title = h.title, period = h.period, minute = h.preferredMinute,
                    repeatMask = h.repeatMask, reminderMinute = h.reminderMinute,
                    timerSeconds = h.timerSeconds, createdDay = h.createdDay,
                    done = day in doneDays,
                    streak = Streaks.current(h.repeatMask, h.createdDay, doneDays, day),
                    dueToday = HabitSchedule.isDue(h.repeatMask, h.createdDay, h.archivedDay, day),
                )
            }
        }

    override suspend fun getDraft(id: Long): HabitDraft? = dao.get(id)?.let {
        HabitDraft(it.id, it.title, it.period, it.preferredMinute, it.repeatMask, it.reminderMinute, it.timerSeconds, it.sourceIdeaId)
    }

    override suspend fun save(draft: HabitDraft): Long {
        val existing = if (draft.id != 0L) dao.get(draft.id) else null
        return if (existing == null) {
            dao.insert(
                HabitEntity(
                    title = draft.title.trim(), period = draft.period, preferredMinute = draft.minute,
                    repeatMask = draft.repeatMask, reminderMinute = draft.reminderMinute,
                    timerSeconds = draft.timerSeconds, sortOrder = dao.maxOrder() + 1,
                    createdDay = clock.today(), archivedDay = null, sourceIdeaId = draft.sourceIdeaId,
                )
            )
        } else {
            dao.update(
                existing.copy(
                    title = draft.title.trim(), period = draft.period, preferredMinute = draft.minute,
                    repeatMask = draft.repeatMask, reminderMinute = draft.reminderMinute,
                    timerSeconds = draft.timerSeconds,
                )
            )
            existing.id
        }
    }

    override suspend fun toggleDone(id: Long, day: Int) {
        if (dao.logCount(id, day) > 0) dao.deleteLog(id, day)
        else dao.upsertLog(HabitLogEntity(id, day, System.currentTimeMillis()))
    }

    override suspend fun archive(id: Long, day: Int) = dao.archive(id, day)

    override suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { i, id -> dao.setOrder(id, i + 1) }
    }
}
