package com.dayflow.app.domain.model

enum class MessageStatus(val label: String) {
    SCHEDULED("Scheduled"),
    TRIGGERED("Triggered"),
    OPENED("Opened"),
    FAILED("Failed"),
}

/**
 * A message the user wants to be reminded to send on WhatsApp at an exact local date and time.
 * DAYFLOW never sends anything: at [day]/[minute] it alerts, and the user presses Send in WhatsApp.
 *
 * [day] (epoch day) and [minute] (minute of day) are wall-clock values, like every other DAYFLOW schedule,
 * so "9:00 on Nov 25" stays 9:00 local time if the timezone changes.
 */
data class ScheduledMessage(
    val id: Long,
    val recipientName: String,
    val phone: String,            // normalized E.164, e.g. +9779812345678
    val occasion: String,
    val message: String,
    val day: Int,
    val minute: Int,
    val status: MessageStatus,
    val createdAt: Long,
    val triggeredAt: Long?,
    val openedAt: Long?,
    val failureReason: String?,
) {
    val displayName: String get() = recipientName.ifBlank { phone }
}

/** What the editor submits. [phone] is whatever the user typed or picked; it is normalized on save. */
data class ScheduledMessageDraft(
    val id: Long = 0,
    val recipientName: String = "",
    val phone: String = "",
    val occasion: String = "Birthday",
    val message: String = "",
    val day: Int = 0,
    val minute: Int = 9 * 60,
)

sealed interface MessageSaveOutcome {
    /** [exactAlarmOk] false = saved, but Android will not guarantee the exact minute (permission missing). */
    data class Saved(val id: Long, val exactAlarmOk: Boolean) : MessageSaveOutcome
    data class Invalid(val problems: List<String>) : MessageSaveOutcome
}

data class MessageSettings(
    val attentionMode: Boolean = false,
    /** Optional, e.g. "+977". Lets locally written numbers (9812345678) be completed automatically. */
    val defaultCountryCode: String = "",
)
