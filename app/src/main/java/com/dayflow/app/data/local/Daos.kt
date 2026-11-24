package com.dayflow.app.data.local

import androidx.room.*
import com.dayflow.app.domain.model.TaskKind
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit WHERE archivedDay IS NULL ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_log")
    fun observeLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit WHERE id = :id")
    suspend fun get(id: Long): HabitEntity?

    @Insert suspend fun insert(e: HabitEntity): Long
    @Update suspend fun update(e: HabitEntity)

    @Query("UPDATE habit SET archivedDay = :day WHERE id = :id")
    suspend fun archive(id: Long, day: Int)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM habit")
    suspend fun maxOrder(): Int

    @Query("UPDATE habit SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(id: Long, order: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLog(l: HabitLogEntity)

    @Query("DELETE FROM habit_log WHERE habitId = :id AND day = :day")
    suspend fun deleteLog(id: Long, day: Int)

    @Query("SELECT COUNT(*) FROM habit_log WHERE habitId = :id AND day = :day")
    suspend fun logCount(id: Long, day: Int): Int

    // ---- history ----
    @Query("SELECT * FROM habit")
    fun observeAllIncludingArchived(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit")
    suspend fun getAllIncludingArchived(): List<HabitEntity>

    @Query("SELECT habitId FROM habit_log WHERE day = :day")
    fun observeDoneIds(day: Int): Flow<List<Long>>

    @Query("SELECT habitId FROM habit_log WHERE day = :day")
    suspend fun doneIds(day: Int): List<Long>
}

@Dao
interface TaskDao {
    // Today's list: planned today, still-open from earlier (overdue), or completed today.
    @Query(
        """SELECT * FROM task WHERE status != 'DROPPED' AND (
            plannedDay = :day
            OR (status = 'OPEN' AND plannedDay < :day)
            OR (status = 'DONE' AND completedDay = :day)
        ) ORDER BY sortOrder, id"""
    )
    fun observeToday(day: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE status = 'OPEN' AND plannedDay > :day ORDER BY plannedDay, sortOrder")
    fun observeUpcoming(day: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE status = 'OPEN' AND plannedDay < :day")
    suspend fun openBefore(day: Int): List<TaskEntity>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Insert suspend fun insert(e: TaskEntity): Long
    @Update suspend fun update(e: TaskEntity)

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM task WHERE kind = :kind")
    suspend fun maxOrder(kind: TaskKind): Int

    @Query("UPDATE task SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(id: Long, order: Int)

    // Same membership rule as observeToday, for any day (history / finalize).
    @Query(
        """SELECT * FROM task WHERE status != 'DROPPED' AND (
            plannedDay = :day
            OR (status = 'OPEN' AND plannedDay < :day)
            OR (status = 'DONE' AND completedDay = :day)
        ) ORDER BY sortOrder, id"""
    )
    suspend fun forDay(day: Int): List<TaskEntity>

    @Query("SELECT MIN(plannedDay) FROM task")
    suspend fun earliestPlannedDay(): Int?
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtask ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtask WHERE taskId = :taskId ORDER BY sortOrder, id")
    suspend fun forTask(taskId: Long): List<SubtaskEntity>

    @Insert suspend fun insertAll(items: List<SubtaskEntity>)

    @Query("DELETE FROM subtask WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)

    @Query("UPDATE subtask SET done = NOT done WHERE id = :id")
    suspend fun toggle(id: Long)
}

@Dao
interface IdeaDao {
    @Query("SELECT * FROM idea ORDER BY pinned DESC, createdAt DESC")
    fun observeAll(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM idea WHERE id = :id")
    suspend fun get(id: Long): IdeaEntity?

    @Insert suspend fun insert(e: IdeaEntity): Long
    @Update suspend fun update(e: IdeaEntity)

    @Query("DELETE FROM idea WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM idea WHERE createdAt >= :from AND createdAt < :to ORDER BY createdAt")
    fun observeCreatedBetween(from: Long, to: Long): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM idea WHERE createdAt >= :from AND createdAt < :to ORDER BY createdAt")
    suspend fun createdBetween(from: Long, to: Long): List<IdeaEntity>

    @Query("SELECT MIN(createdAt) FROM idea")
    suspend fun earliestCreatedAt(): Long?
}

@Dao
interface RoadmapDao {
    @Query("SELECT * FROM roadmap_item WHERE active = 1 ORDER BY startMinute, sortOrder")
    fun observeItems(): Flow<List<RoadmapItemEntity>>

    @Query("SELECT * FROM roadmap_item WHERE active = 1")
    suspend fun activeItems(): List<RoadmapItemEntity>

    @Query("SELECT * FROM roadmap_item WHERE id = :id")
    suspend fun getItem(id: Long): RoadmapItemEntity?

    @Insert suspend fun insertItem(e: RoadmapItemEntity): Long
    @Update suspend fun updateItem(e: RoadmapItemEntity)

    /** Soft delete: past days keep their logs. */
    @Query("UPDATE roadmap_item SET active = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM roadmap_item")
    suspend fun maxOrder(): Int

    @Query("SELECT * FROM roadmap_day WHERE day = :day")
    fun observeDay(day: Int): Flow<List<RoadmapDayEntity>>

    @Query("SELECT * FROM roadmap_day WHERE itemId = :itemId AND day = :day")
    suspend fun getDay(itemId: Long, day: Int): RoadmapDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDay(e: RoadmapDayEntity)

    @Query("DELETE FROM roadmap_day WHERE itemId = :itemId AND day = :day")
    suspend fun clearDay(itemId: Long, day: Int)
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarm ORDER BY minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarm ORDER BY minute, id")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarm WHERE id = :id")
    suspend fun get(id: Long): AlarmEntity?

    @Insert suspend fun insert(e: AlarmEntity): Long
    @Update suspend fun update(e: AlarmEntity)

    @Query("DELETE FROM alarm WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TimerDao {
    /** The one timer the UI cares about: running, paused, or finished-but-not-yet-dismissed. */
    @Query("SELECT * FROM timer_session WHERE state IN ('RUNNING','PAUSED','FINISHED') ORDER BY id DESC LIMIT 1")
    fun observeCurrent(): Flow<TimerSessionEntity?>

    @Query("SELECT * FROM timer_session WHERE state IN ('RUNNING','PAUSED','FINISHED') ORDER BY id DESC LIMIT 1")
    suspend fun current(): TimerSessionEntity?

    @Query("SELECT * FROM timer_session WHERE id = :id")
    suspend fun get(id: Long): TimerSessionEntity?

    @Insert suspend fun insert(e: TimerSessionEntity): Long
    @Update suspend fun update(e: TimerSessionEntity)

    /** Time actually run by sessions that started in [from, to). Running/paused sessions count once closed. */
    @Query("SELECT COALESCE(SUM(focusMs), 0) FROM timer_session WHERE startedAt >= :from AND startedAt < :to")
    fun observeFocusBetween(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(focusMs), 0) FROM timer_session WHERE startedAt >= :from AND startedAt < :to")
    suspend fun focusBetween(from: Long, to: Long): Long
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM day_summary WHERE day BETWEEN :from AND :to ORDER BY day")
    fun observeRange(from: Int, to: Int): Flow<List<DaySummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSummary(e: DaySummaryEntity)

    @Query("SELECT * FROM day_note WHERE day = :day")
    fun observeNote(day: Int): Flow<DayNoteEntity?>

    @Query("SELECT * FROM day_note WHERE day = :day")
    suspend fun note(day: Int): DayNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertNote(e: DayNoteEntity)

    @Query("DELETE FROM day_note WHERE day = :day")
    suspend fun deleteNote(day: Int)
}

/** Whole-database read/replace, used only by Backup & Restore. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM habit") suspend fun habits(): List<HabitEntity>
    @Query("SELECT * FROM habit_log") suspend fun habitLogs(): List<HabitLogEntity>
    @Query("SELECT * FROM task") suspend fun tasks(): List<TaskEntity>
    @Query("SELECT * FROM subtask") suspend fun subtasks(): List<SubtaskEntity>
    @Query("SELECT * FROM idea") suspend fun ideas(): List<IdeaEntity>
    @Query("SELECT * FROM roadmap_item") suspend fun roadmapItems(): List<RoadmapItemEntity>
    @Query("SELECT * FROM roadmap_day") suspend fun roadmapDays(): List<RoadmapDayEntity>
    @Query("SELECT * FROM alarm") suspend fun alarms(): List<AlarmEntity>
    @Query("SELECT * FROM day_summary") suspend fun daySummaries(): List<DaySummaryEntity>
    @Query("SELECT * FROM day_note") suspend fun dayNotes(): List<DayNoteEntity>
    @Query("SELECT * FROM scheduled_message") suspend fun scheduledMessages(): List<ScheduledMessageEntity>

    @Query("DELETE FROM subtask") suspend fun clearSubtasks()
    @Query("DELETE FROM task") suspend fun clearTasks()
    @Query("DELETE FROM habit_log") suspend fun clearHabitLogs()
    @Query("DELETE FROM habit") suspend fun clearHabits()
    @Query("DELETE FROM idea") suspend fun clearIdeas()
    @Query("DELETE FROM roadmap_day") suspend fun clearRoadmapDays()
    @Query("DELETE FROM roadmap_item") suspend fun clearRoadmapItems()
    @Query("DELETE FROM alarm") suspend fun clearAlarms()
    @Query("DELETE FROM day_summary") suspend fun clearDaySummaries()
    @Query("DELETE FROM day_note") suspend fun clearDayNotes()
    @Query("DELETE FROM timer_session") suspend fun clearTimers()
    @Query("DELETE FROM scheduled_message") suspend fun clearScheduledMessages()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHabits(l: List<HabitEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHabitLogs(l: List<HabitLogEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTasks(l: List<TaskEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSubtasks(l: List<SubtaskEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putIdeas(l: List<IdeaEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRoadmapItems(l: List<RoadmapItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRoadmapDays(l: List<RoadmapDayEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAlarms(l: List<AlarmEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDaySummaries(l: List<DaySummaryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDayNotes(l: List<DayNoteEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putScheduledMessages(l: List<ScheduledMessageEntity>)
}

@Dao
interface ScheduledMessageDao {
    @Query("SELECT * FROM scheduled_message ORDER BY day, minute, id")
    fun observeAll(): Flow<List<ScheduledMessageEntity>>

    @Query("SELECT * FROM scheduled_message WHERE id = :id")
    suspend fun get(id: Long): ScheduledMessageEntity?

    @Query("SELECT * FROM scheduled_message WHERE status = 'SCHEDULED'")
    suspend fun scheduled(): List<ScheduledMessageEntity>

    @Insert suspend fun insert(e: ScheduledMessageEntity): Long
    @Update suspend fun update(e: ScheduledMessageEntity)

    @Query("DELETE FROM scheduled_message WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Atomic SCHEDULED -> TRIGGERED. Returns the number of rows changed: 1 = this caller won and must alert,
     * 0 = already triggered / opened / edited / deleted. This is what makes duplicate alarm deliveries harmless.
     */
    @Query("UPDATE scheduled_message SET status = 'TRIGGERED', triggeredAt = :at, failureReason = NULL WHERE id = :id AND status = 'SCHEDULED'")
    suspend fun markTriggered(id: Long, at: Long): Int

    @Query("UPDATE scheduled_message SET status = 'OPENED', openedAt = :at, failureReason = NULL WHERE id = :id")
    suspend fun markOpened(id: Long, at: Long): Int

    @Query("UPDATE scheduled_message SET status = 'FAILED', failureReason = :reason WHERE id = :id")
    suspend fun markFailed(id: Long, reason: String): Int
}
