package com.dayflow.app.domain.repo

import com.dayflow.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    /** All active habits with done/streak/due computed for [day]. */
    fun observeHabits(day: Int): Flow<List<Habit>>
    suspend fun getDraft(id: Long): HabitDraft?
    suspend fun save(draft: HabitDraft): Long
    suspend fun toggleDone(id: Long, day: Int)
    /** "Delete": removes from the routine but keeps every past log. */
    suspend fun archive(id: Long, day: Int)
    suspend fun reorder(orderedIds: List<Long>)
}

interface TaskRepository {
    /** Today's list for one lane: planned today + still-open earlier items + done today. */
    fun observeToday(day: Int): Flow<List<TaskItem>>
    fun observeUpcoming(day: Int): Flow<List<TaskItem>>
    suspend fun getDraft(id: Long): TaskDraft?
    suspend fun getItem(id: Long): TaskItem?
    suspend fun save(draft: TaskDraft): Long
    suspend fun toggleDone(id: Long, day: Int)
    suspend fun toggleSubtask(subtaskId: Long)
    suspend fun delete(id: Long)
    suspend fun reschedule(id: Long, day: Int, countRollover: Boolean = false)
    suspend fun drop(id: Long)
    suspend fun reorder(orderedIds: List<Long>)
    suspend fun changeKind(id: Long, kind: TaskKind)
    suspend fun openBefore(day: Int): List<TaskItem>
}

interface IdeaRepository {
    fun observeAll(): Flow<List<Idea>>
    suspend fun getDraft(id: Long): IdeaDraft?
    suspend fun get(id: Long): Idea?
    suspend fun save(draft: IdeaDraft): Long
    suspend fun togglePin(id: Long)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun markConverted(id: Long, kind: ItemKind)
    suspend fun delete(id: Long)
}

interface RoadmapRepository {
    fun observeItems(): Flow<List<RoadmapItem>>
    fun observeDayLogs(day: Int): Flow<List<RoadmapDayLog>>
    suspend fun getItem(id: Long): RoadmapItem?
    suspend fun activeItems(): List<RoadmapItem>
    suspend fun save(draft: RoadmapDraft): Long
    suspend fun delete(id: Long)
    suspend fun setDone(itemId: Long, day: Int, done: Boolean)
    suspend fun skip(itemId: Long, day: Int)
    suspend fun reschedule(itemId: Long, day: Int, start: Int, end: Int)
    suspend fun clearDay(itemId: Long, day: Int)
}

/** Lets use cases span several repositories atomically without knowing about Room. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

interface HistoryRepository {
    /** Everything about one day (live): summary, per-lane items, ideas captured, tracked time, reflection note. */
    fun observeDetail(day: Int): Flow<DayDetail>
    /** Finalized summaries for past days in [from, to], plus today's live summary when it falls in range. */
    fun observeSummaries(from: Int, to: Int): Flow<List<DaySummary>>
    fun observeHabitHistory(today: Int): Flow<List<HabitHistory>>
    /** Snapshots every unfinalized day before [today]. Run BEFORE the task rollover moves things around. */
    suspend fun finalizeBefore(today: Int)
    suspend fun saveNote(day: Int, text: String)
}
