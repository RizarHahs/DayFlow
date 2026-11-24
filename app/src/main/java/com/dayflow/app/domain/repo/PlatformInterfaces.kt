package com.dayflow.app.domain.repo

import com.dayflow.app.domain.model.*
import kotlinx.coroutines.flow.Flow

/** UI-facing alarm API. Saving always (re)schedules the real Android alarm. */
interface AlarmController {
    fun observeAlarms(): Flow<List<Alarm>>
    suspend fun get(id: Long): Alarm?
    suspend fun save(draft: AlarmDraft): Long
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun delete(id: Long)
    /** Rings a throw-away alarm after [delayMs] through the exact same pipeline as real alarms. */
    fun scheduleTest(delayMs: Long): Boolean
}

interface TimerController {
    val current: Flow<TimerSession?>
    fun bootCount(): Int
    suspend fun start(label: String, durationMs: Long, target: TimerTarget? = null, targetId: Long? = null)
    suspend fun pause()
    suspend fun resume()
    suspend fun cancel()
    suspend fun reset()
    suspend fun dismissFinished()
}

/** UI-facing API for scheduled WhatsApp messages. Saving always (re)arms the real Android alarm. */
interface ScheduledMessageController {
    fun observe(): Flow<List<ScheduledMessage>>
    suspend fun get(id: Long): ScheduledMessage?
    suspend fun getDraft(id: Long): ScheduledMessageDraft?
    suspend fun save(draft: ScheduledMessageDraft): MessageSaveOutcome
    /** Cancels the alarm and the notification, then deletes. Safe to call for an id that no longer exists. */
    suspend fun delete(id: Long)
    suspend fun markOpened(id: Long)
    suspend fun markFailed(id: Long, reason: String)
}
