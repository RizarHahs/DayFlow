package com.dayflow.app

import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.usecase.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.time.LocalDate
import java.time.ZoneId

/** Scheduled WhatsApp messages: E.164 validation, wa.me encoding (emoji/newlines), scheduling validation, occasions. */
class ScheduledMessageRulesTest {
    private fun check(name: String, ok: Boolean) = assertTrue(name, ok)

    @Test
    fun scheduledMessageRules() {

        // phone normalization
        check("plain E.164", PhoneNumber.normalize("+9779812345678") == "+9779812345678")
        check("spaces/dashes", PhoneNumber.normalize("+977 981-234-5678") == "+9779812345678")
        check("brackets/dots", PhoneNumber.normalize("+1 (415) 555.2671") == "+14155552671")
        check("00 prefix", PhoneNumber.normalize("009779812345678") == "+9779812345678")
        check("local without default code is rejected", PhoneNumber.normalize("9812345678") == null)
        check("local with default code", PhoneNumber.normalize("9812345678", "+977") == "+9779812345678")
        check("national trunk 0 stripped", PhoneNumber.normalize("07911 123456", "44") == "+447911123456")
        check("too short", PhoneNumber.normalize("+12345") == null)
        check("too long (16 digits)", PhoneNumber.normalize("+1234567890123456") == null)
        check("max length 15 ok", PhoneNumber.normalize("+123456789012345") == "+123456789012345")
        check("leading zero after + rejected", PhoneNumber.normalize("+0123456789") == null)
        check("letters rejected", PhoneNumber.normalize("+97798abc45678") == null)
        check("blank rejected", PhoneNumber.normalize("   ") == null)
        check("unicode nbsp tolerated", PhoneNumber.normalize("+977\u00A09812345678") == "+9779812345678")

        // link encoding
        val text = "Happy Birthday! \uD83C\uDF82\u2764\uFE0F Wishing you an amazing year ahead!\nLine two: 100% sure & ready, 'quoted' (ok)? #yes + more"
        val link = WhatsAppLink.build("+9779812345678", text)
        check("wa.me host and digits only", link.startsWith("https://wa.me/9779812345678?text="))
        check("no raw '+' of the phone number", !link.substringBefore("?").contains("+"))
        val enc = link.substringAfter("?text=")
        check("space -> %20", enc.contains("Happy%20Birthday"))
        check("newline -> %0A", enc.contains("%0A") && !enc.contains("\n"))
        check("emoji percent-encoded as UTF-8", enc.contains("%F0%9F%8E%82"))
        check("ampersand and hash escaped", enc.contains("%26") && enc.contains("%23") && !enc.contains("&") && !enc.contains("#"))
        check("round-trips exactly (emoji, newline, punctuation)", URLDecoder.decode(enc.replace("+", "%2B"), "UTF-8") == text)
        check("plus sign survives", URLDecoder.decode(enc.replace("+", "%2B"), "UTF-8").contains("+ more"))

        // validation
        val zone = ZoneId.of("Asia/Kathmandu")
        val today = LocalDate.of(2026, 11, 25).toEpochDay().toInt()
        val now = RemindersPlan.atMillis(today, 8 * 60, zone)
        val ok = ScheduledMessageDraft(recipientName = "Asha", phone = "+9779812345678", occasion = "Birthday", message = "Hi", day = today, minute = 9 * 60)
        check("valid draft", MessageValidation.validate(ok, now, zone, "").isEmpty())
        check("past time rejected", MessageValidation.validate(ok.copy(minute = 7 * 60), now, zone, "").any { it.contains("future") })
        check("past day rejected", MessageValidation.validate(ok.copy(day = today - 1), now, zone, "").isNotEmpty())
        check("same minute (not strictly future) rejected", MessageValidation.validate(ok.copy(minute = 8 * 60), now, zone, "").isNotEmpty())
        check("blank message rejected", MessageValidation.validate(ok.copy(message = "  "), now, zone, "").any { it.contains("message") })
        check("too long rejected", MessageValidation.validate(ok.copy(message = "x".repeat(1001)), now, zone, "").any { it.contains("too long") })
        check("1000 chars ok", MessageValidation.validate(ok.copy(message = "x".repeat(1000)), now, zone, "").isEmpty())
        check("bad phone rejected", MessageValidation.validate(ok.copy(phone = "98123"), now, zone, "").any { it.contains("country code") })
        check("local phone ok with default code", MessageValidation.validate(ok.copy(phone = "9812345678"), now, zone, "+977").isEmpty())
        check("blank occasion rejected", MessageValidation.validate(ok.copy(occasion = ""), now, zone, "").isNotEmpty())
        check("invalid minute rejected", MessageValidation.validate(ok.copy(minute = 2000), now, zone, "").isNotEmpty())
        check("several problems reported together", MessageValidation.validate(ScheduledMessageDraft(), now, zone, "").size >= 3)

        // occasions
        check("birthday template matches the brief", Occasions.templateFor("Birthday") == "Happy Birthday! \uD83C\uDF82\u2764\uFE0F Wishing you an amazing year ahead!")
        check("many occasions + custom", Occasions.labels.size >= 9 && Occasions.labels.last() == Occasions.CUSTOM)
        check("custom has no template", Occasions.templateFor("Mum's retirement") == null && !Occasions.isPreset("Mum's retirement"))
        check("display name falls back to number", ScheduledMessage(1, "", "+9779812345678", "x", "m", today, 0, MessageStatus.SCHEDULED, 0, null, null, null).displayName == "+9779812345678")


    }
}
