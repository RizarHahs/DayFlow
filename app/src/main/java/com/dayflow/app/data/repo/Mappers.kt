package com.dayflow.app.data.repo

import com.dayflow.app.data.local.*
import com.dayflow.app.domain.model.*

internal fun parseTags(raw: String): List<String> =
    raw.split(' ').map { it.trim() }.filter { it.isNotEmpty() }

internal fun IdeaEntity.toDomain() = Idea(
    id = id, text = text, description = description, pinned = pinned, archived = archived,
    tags = parseTags(tags), createdAt = createdAt, updatedAt = updatedAt, convertedTo = convertedToKind,
)

internal fun TaskEntity.toDomain(subtasks: List<Subtask>, today: Int) = TaskItem(
    id = id, kind = kind, title = title, description = description,
    done = status == TaskStatus.DONE, priority = priority,
    plannedDay = plannedDay, dueDay = dueDay, startMinute = startMinute, endMinute = endMinute,
    estimateMin = estimateMin, reminderMinute = reminderMinute, timerSeconds = timerSeconds,
    rolloverPolicy = rolloverPolicy, rolloverCount = rolloverCount, subtasks = subtasks,
    overdue = status == TaskStatus.OPEN && (plannedDay < today || (dueDay != null && dueDay < today)),
)

internal fun SubtaskEntity.toDomain() = Subtask(id, title, done)

internal fun RoadmapItemEntity.toDomain() = RoadmapItem(
    id, title, startMinute, endMinute, repeatMask, linkedHabitId, linkedTaskId,
)

internal fun RoadmapDayEntity.toDomain() = RoadmapDayLog(itemId, day, status, overrideStart, overrideEnd)

internal fun AlarmEntity.toDomain() = Alarm(
    id, minute, repeatMask, label, enabled, kind, soundUri, vibrate, snoozeMin,
    linkedHabitId, linkedRoadmapId, nextTriggerAt,
)

internal fun TimerSessionEntity.toDomain() = TimerSession(
    id, label, targetType, targetId, durationMs, state, endElapsed, endEpoch, remainingMsPaused, bootCount,
)

internal fun ScheduledMessageEntity.toDomain() = ScheduledMessage(
    id, recipientName, phone, occasion, message, day, minute, status, createdAt, triggeredAt, openedAt, failureReason,
)
