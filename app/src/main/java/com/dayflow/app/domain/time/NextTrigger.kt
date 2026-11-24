package com.dayflow.app.domain.time

import java.time.Instant
import java.time.ZoneId

object NextTrigger {
    /**
     * Next wall-clock instant (epoch ms) strictly after [nowMillis] at [minute] of day,
     * on a weekday allowed by [repeatMask] (bit0 = Monday; 0 = every day / one-shot).
     * Evaluated in [zone], so DST and timezone changes are handled by re-running this.
     */
    fun compute(minute: Int, repeatMask: Int, nowMillis: Long, zone: ZoneId): Long {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        for (i in 0..8) {
            val date = today.plusDays(i.toLong())
            val allowed = repeatMask == 0 || ((repeatMask shr (date.dayOfWeek.value - 1)) and 1) == 1
            if (!allowed) continue
            val at = date.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
            if (at > nowMillis) return at
        }
        return nowMillis + 24L * 3_600_000L
    }
}
