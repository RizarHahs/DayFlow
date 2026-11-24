package com.dayflow.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.dayflow.app.domain.model.*

/** Days are epoch-days (Int); times are minute-of-day (Int); timestamps are epoch millis (Long). */

@Serializable
@Entity(tableName = "habit")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val period: Period,
    val preferredMinute: Int?,
    val repeatMask: Int,
    val reminderMinute: Int?,
    val timerSeconds: Int?,
    val sortOrder: Int,
    val createdDay: Int,
    val archivedDay: Int?,
    val sourceIdeaId: Long?,
)

/** Row present = completed that day. Absence = not completed. History is never rewritten. */
@Serializable
@Entity(
    tableName = "habit_log",
    primaryKeys = ["habitId", "day"],
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")],
)
data class HabitLogEntity(val habitId: Long, val day: Int, val completedAt: Long)

@Serializable
@Entity(tableName = "task", indices = [Index("kind"), Index("plannedDay"), Index("status")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: TaskKind,
    val title: String,
    val description: String,
    val priority: Priority?,
    val plannedDay: Int,
    val dueDay: Int?,
    val startMinute: Int?,
    val endMinute: Int?,
    val estimateMin: Int?,
    val reminderMinute: Int?,
    val timerSeconds: Int?,
    val status: TaskStatus,
    val completedAt: Long?,
    val completedDay: Int?,
    val rolloverPolicy: RolloverPolicy,
    val rolloverCount: Int,
    val sortOrder: Int,
    val sourceIdeaId: Long?,
    val promotedFromKind: TaskKind?,
    val createdAt: Long,
)

@Serializable
@Entity(
    tableName = "subtask",
    foreignKeys = [ForeignKey(TaskEntity::class, ["id"], ["taskId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taskId")],
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val done: Boolean,
    val sortOrder: Int,
)

/** Tags are stored space-separated (lowercase, no '#'). Search is in-memory; fine at personal scale. */
@Serializable
@Entity(tableName = "idea")
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean,
    val archived: Boolean,
    val tags: String,
    val convertedToKind: ItemKind?,
)

@Serializable
@Entity(tableName = "roadmap_item")
data class RoadmapItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startMinute: Int,
    val endMinute: Int,
    val repeatMask: Int,
    val linkedHabitId: Long?,
    val linkedTaskId: Long?,
    val sortOrder: Int,
    val active: Boolean,
)

@Serializable
@Entity(tableName = "roadmap_day", primaryKeys = ["itemId", "day"], indices = [Index("day")])
data class RoadmapDayEntity(
    val itemId: Long,
    val day: Int,
    val status: DayLogStatus,
    val overrideStart: Int?,
    val overrideEnd: Int?,
    val completedAt: Long?,
)

@Serializable
@Entity(tableName = "alarm")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val minute: Int,
    val repeatMask: Int,
    val label: String,
    val enabled: Boolean,
    val kind: AlarmKind,
    val soundUri: String?,
    val vibrate: Boolean,
    val snoozeMin: Int,
    val linkedHabitId: Long?,
    val linkedRoadmapId: Long?,
    val nextTriggerAt: Long?,
)

@Serializable
@Entity(tableName = "timer_session")
data class TimerSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val targetType: TimerTarget?,
    val targetId: Long?,
    val durationMs: Long,
    val state: TimerState,
    val startedElapsed: Long,
    val endElapsed: Long,
    val endEpoch: Long,
    val remainingMsPaused: Long,
    val bootCount: Int,
    val startedAt: Long,
    /** Time actually run (duration minus what was left) once the session ends. Feeds Phase 5 "tracked time". */
    val focusMs: Long,
)

/** One row per finalized past day. Written once at rollover; today is always computed live. */
@Serializable
@Entity(tableName = "day_summary")
data class DaySummaryEntity(
    @PrimaryKey val day: Int,
    val habitsDone: Int,
    val habitsTotal: Int,
    val importantDone: Int,
    val importantTotal: Int,
    val normalDone: Int,
    val normalTotal: Int,
    val extraDone: Int,
    val extraTotal: Int,
    val score: Float,
    val trackedMs: Long,
    val ideasCaptured: Int,
    val dayComplete: Boolean,
    val finalizedAt: Long,
)

/** The user's own reflection for a day (Daily review). Kept apart so finalizing never touches it. */
@Serializable
@Entity(tableName = "day_note")
data class DayNoteEntity(
    @PrimaryKey val day: Int,
    val text: String,
    val updatedAt: Long,
)

/** Phase 6 / Scheduled WhatsApp messages. Additive: one new table, no change to any existing table. */
@Serializable
@Entity(tableName = "scheduled_message")
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientName: String,
    val phone: String,
    val occasion: String,
    val message: String,
    val day: Int,
    val minute: Int,
    val status: MessageStatus,
    val createdAt: Long,
    val triggeredAt: Long?,
    val openedAt: Long?,
    val failureReason: String?,
)
