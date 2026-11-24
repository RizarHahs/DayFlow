package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.time.isScheduled

data class TimeBlock(val id: Long, val title: String, val start: Int, val end: Int)

object Conflicts {
    fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int) = aStart < bEnd && bStart < aEnd

    /** Recurring items clash only when they share a weekday *and* their times overlap. */
    fun forRecurring(draft: RoadmapDraft, others: List<RoadmapItem>): List<TimeBlock> =
        others.filter {
            it.id != draft.id &&
                (it.repeatMask and draft.repeatMask) != 0 &&
                overlaps(draft.startMinute, draft.endMinute, it.startMinute, it.endMinute)
        }.map { TimeBlock(it.id, it.title, it.startMinute, it.endMinute) }

    fun forSlot(itemId: Long, start: Int, end: Int, slots: List<RoadmapSlot>): List<TimeBlock> =
        slots.filter {
            it.itemId != itemId && it.status != SlotStatus.SKIPPED &&
                overlaps(start, end, it.startMinute, it.endMinute)
        }.map { TimeBlock(it.itemId, it.title, it.startMinute, it.endMinute) }
}

object RoadmapSlots {
    /**
     * @param nowMinute minute of day for [day]: real minute for today, 1440 for past days
     *   (everything unfinished is missed), -1 for future days (everything upcoming).
     */
    fun build(
        day: Int,
        nowMinute: Int,
        items: List<RoadmapItem>,
        logs: List<RoadmapDayLog>,
        doneHabitIds: Set<Long>,
        doneTaskIds: Set<Long>,
    ): List<RoadmapSlot> {
        val logBy = logs.associateBy { it.itemId }
        val raw = items.filter { isScheduled(it.repeatMask, day) }.map { item ->
            val log = logBy[item.id]
            val start = log?.overrideStart ?: item.startMinute
            val end = log?.overrideEnd ?: item.endMinute
            val linkedDone = (item.linkedHabitId in doneHabitIds) || (item.linkedTaskId in doneTaskIds)
            val status = when {
                log?.status == DayLogStatus.SKIPPED -> SlotStatus.SKIPPED
                log?.status == DayLogStatus.DONE || linkedDone -> SlotStatus.DONE
                end <= nowMinute -> SlotStatus.MISSED
                start <= nowMinute -> SlotStatus.CURRENT
                else -> SlotStatus.UPCOMING
            }
            RoadmapSlot(
                itemId = item.id, title = item.title, startMinute = start, endMinute = end,
                status = status, rescheduled = log?.overrideStart != null,
                conflict = false, linkedHabitId = item.linkedHabitId, linkedTaskId = item.linkedTaskId,
            )
        }.sortedBy { it.startMinute }

        return raw.map { s ->
            val clash = s.status != SlotStatus.SKIPPED && raw.any {
                it.itemId != s.itemId && it.status != SlotStatus.SKIPPED &&
                    Conflicts.overlaps(s.startMinute, s.endMinute, it.startMinute, it.endMinute)
            }
            s.copy(conflict = clash)
        }
    }
}
