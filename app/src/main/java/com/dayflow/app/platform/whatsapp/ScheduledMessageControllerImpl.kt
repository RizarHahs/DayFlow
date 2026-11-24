package com.dayflow.app.platform.whatsapp

import com.dayflow.app.data.local.ScheduledMessageDao
import com.dayflow.app.data.local.ScheduledMessageEntity
import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.data.repo.toDomain
import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.repo.ScheduledMessageController
import com.dayflow.app.domain.usecase.MessageValidation
import com.dayflow.app.domain.usecase.PhoneNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledMessageControllerImpl @Inject constructor(
    private val dao: ScheduledMessageDao,
    private val scheduler: ScheduledMessageScheduler,
    private val notifier: MessageNotifier,
    private val prefs: AppPrefs,
) : ScheduledMessageController {

    override fun observe(): Flow<List<ScheduledMessage>> = dao.observeAll().map { l -> l.map { it.toDomain() } }

    override suspend fun get(id: Long): ScheduledMessage? = dao.get(id)?.toDomain()

    override suspend fun getDraft(id: Long): ScheduledMessageDraft? = dao.get(id)?.let {
        ScheduledMessageDraft(it.id, it.recipientName, it.phone, it.occasion, it.message, it.day, it.minute)
    }

    /**
     * Validates, stores (status back to SCHEDULED, so an edit is also a reschedule), cancels whatever was armed
     * or shown for the old version, then arms the new alarm.
     */
    override suspend fun save(draft: ScheduledMessageDraft): MessageSaveOutcome {
        val settings = prefs.messageSettingsNow()
        val problems = MessageValidation.validate(draft, System.currentTimeMillis(), ZoneId.systemDefault(), settings.defaultCountryCode)
        if (problems.isNotEmpty()) return MessageSaveOutcome.Invalid(problems)
        val phone = PhoneNumber.normalize(draft.phone, settings.defaultCountryCode)
            ?: return MessageSaveOutcome.Invalid(listOf("Enter the number with its country code."))

        val existing = if (draft.id != 0L) dao.get(draft.id) else null
        val base = ScheduledMessageEntity(
            id = existing?.id ?: 0,
            recipientName = draft.recipientName.trim(), phone = phone, occasion = draft.occasion.trim(),
            message = draft.message, day = draft.day, minute = draft.minute,
            status = MessageStatus.SCHEDULED,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            triggeredAt = null, openedAt = null, failureReason = null,
        )
        val id = if (existing == null) dao.insert(base) else { dao.update(base); base.id }
        scheduler.cancel(id)
        notifier.cancel(id)
        val exact = scheduler.schedule(base.copy(id = id))
        return MessageSaveOutcome.Saved(id, exact)
    }

    override suspend fun delete(id: Long) {
        scheduler.cancel(id)
        notifier.cancel(id)
        dao.delete(id)
    }

    override suspend fun markOpened(id: Long) {
        dao.markOpened(id, System.currentTimeMillis())
        scheduler.cancel(id)        // opened early from the list: the pending alarm must not fire later
        notifier.cancel(id)
    }

    override suspend fun markFailed(id: Long, reason: String) {
        dao.markFailed(id, reason)
    }
}
