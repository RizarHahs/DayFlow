package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.ScheduledMessageDraft
import com.dayflow.app.domain.time.dayLabel
import java.net.URLEncoder
import java.time.ZoneId

object PhoneNumber {
    private val e164 = Regex("^\\+[1-9][0-9]{7,14}$")

    /**
     * Returns the number in E.164 (+ then 8 to 15 digits, no leading zero), or null if it cannot be made valid.
     * Accepts spaces, dashes, dots and brackets, "00" instead of "+", and, if [defaultCountryCode] is given,
     * nationally written numbers (a single leading 0 is treated as the national trunk prefix).
     */
    fun normalize(raw: String, defaultCountryCode: String = ""): String? {
        val cleaned = raw.trim().replace(Regex("[\\s\\-().\\u00A0]"), "")
        if (cleaned.isEmpty()) return null
        val candidate = when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("00") -> "+" + cleaned.substring(2)
            else -> {
                val cc = defaultCountryCode.trim().replace(Regex("[^0-9]"), "")
                if (cc.isEmpty()) return null
                "+" + cc + cleaned.removePrefix("0")
            }
        }
        if (candidate.drop(1).any { !it.isDigit() }) return null
        return candidate.takeIf { e164.matches(it) }
    }
}

object WhatsAppLink {
    /**
     * Official click-to-chat link: https://wa.me/<digits>?text=<encoded>. The number has no "+".
     * Text is UTF-8 percent-encoded with %20 for spaces and %0A for newlines, so emojis, punctuation and
     * line breaks survive. This only PREFILLS the chat box; the user presses Send.
     */
    fun build(e164: String, text: String): String {
        val digits = e164.removePrefix("+")
        val encoded = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
        return "https://wa.me/$digits?text=$encoded"
    }
}

data class OccasionPreset(val label: String, val template: String)

object Occasions {
    const val CUSTOM = "Custom"

    val presets: List<OccasionPreset> = listOf(
        OccasionPreset("Birthday", "Happy Birthday! \uD83C\uDF82\u2764\uFE0F Wishing you an amazing year ahead!"),
        OccasionPreset("Anniversary", "Happy Anniversary! \uD83E\uDD42 Wishing you both many more years of happiness together."),
        OccasionPreset("Congratulations", "Congratulations! \uD83C\uDF89 So proud of you. You truly deserve this."),
        OccasionPreset("New Year", "Happy New Year! \u2728 Wishing you health, joy and success all year long."),
        OccasionPreset("Festival greeting", "Wishing you and your family a joyful festival full of light and happiness! \uD83E\uDE94"),
        OccasionPreset("Get well soon", "Thinking of you. Get well soon! \uD83C\uDF3C Sending you strength and good wishes."),
        OccasionPreset("Thank you", "Thank you so much, I really appreciate everything you did. \uD83D\uDE4F"),
        OccasionPreset("Good luck", "Good luck today! \uD83C\uDF40 You've got this."),
        OccasionPreset("Thinking of you", "Just thinking of you and wanted to say hello. \uD83D\uDC9B"),
    )

    val labels: List<String> get() = presets.map { it.label } + CUSTOM

    fun templateFor(occasion: String): String? = presets.firstOrNull { it.label == occasion }?.template
    fun isPreset(occasion: String): Boolean = presets.any { it.label == occasion }
}

object MessageValidation {
    const val MAX_LENGTH = 1000
    private const val MIN_LEAD_MS = 5_000L

    /**
     * @return human-readable problems; empty = OK. The trigger time must be in the future, because a
     * message scheduled for a moment that has already passed could never alert.
     */
    fun validate(
        draft: ScheduledMessageDraft,
        nowMillis: Long,
        zone: ZoneId,
        defaultCountryCode: String,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (PhoneNumber.normalize(draft.phone, defaultCountryCode) == null) {
            problems += "Enter the number with its country code, like +977 9812345678."
        }
        if (draft.message.isBlank()) problems += "Write the message you want to send."
        if (draft.message.length > MAX_LENGTH) problems += "The message is too long (max $MAX_LENGTH characters)."
        if (draft.occasion.isBlank()) problems += "Choose or type an occasion."
        if (draft.minute !in 0..1439) problems += "Pick a valid time."
        else if (RemindersPlan.atMillis(draft.day, draft.minute, zone) < nowMillis + MIN_LEAD_MS) {
            problems += "Pick a date and time in the future."
        }
        return problems
    }

    /** Short line for lists: "Today, 21:00" style is built by the UI; this is the plain-text fallback. */
    fun whenLabel(day: Int, today: Int): String = dayLabel(day, today)
}
