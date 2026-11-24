package com.dayflow.app.domain.model

enum class AlarmKind(val label: String) {
    WAKE("Wake-up"), HABIT("Habit"), ROADMAP("Activity"), REVIEW("Daily review"), CUSTOM("Custom")
}

enum class AlarmStatus { ARMED, NEEDS_PERMISSION, DISABLED }

data class Alarm(
    val id: Long,
    val minute: Int,
    val repeatMask: Int,          // 0 = one-shot
    val label: String,
    val enabled: Boolean,
    val kind: AlarmKind,
    val soundUri: String?,        // null = system default alarm sound
    val vibrate: Boolean,
    val snoozeMin: Int,
    val linkedHabitId: Long?,
    val linkedRoadmapId: Long?,
    val nextTriggerAt: Long?,
)

data class AlarmDraft(
    val id: Long = 0,
    val minute: Int = 7 * 60,
    val repeatMask: Int = ALL_DAYS,
    val label: String = "",
    val kind: AlarmKind = AlarmKind.WAKE,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeMin: Int = 9,
    val linkedHabitId: Long? = null,
    val linkedRoadmapId: Long? = null,
    val enabled: Boolean = true,
)

enum class TimerState { RUNNING, PAUSED, FINISHED, CANCELLED, DISMISSED }
enum class TimerTarget { HABIT, TASK, ACTIVITY }

/**
 * One countdown. Time is anchored two ways (see Phase 1 timer strategy):
 *  - [endElapsed] on SystemClock.elapsedRealtime(): monotonic, immune to clock changes, valid within one boot
 *  - [endEpoch] wall clock: used only when the phone rebooted ([bootCount] differs)
 */
data class TimerSession(
    val id: Long,
    val label: String,
    val targetType: TimerTarget?,
    val targetId: Long?,
    val durationMs: Long,
    val state: TimerState,
    val endElapsed: Long,
    val endEpoch: Long,
    val remainingMsPaused: Long,
    val bootCount: Int,
) {
    fun remainingMs(nowElapsed: Long, nowEpoch: Long, currentBootCount: Int): Long = when (state) {
        TimerState.PAUSED -> remainingMsPaused
        TimerState.RUNNING ->
            (if (bootCount == currentBootCount) endElapsed - nowElapsed else endEpoch - nowEpoch).coerceAtLeast(0L)
        else -> 0L
    }
}
