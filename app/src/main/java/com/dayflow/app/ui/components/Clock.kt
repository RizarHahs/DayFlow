package com.dayflow.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Ticks on the minute boundary. Re-reads the system zone every tick, so timezone changes are picked up. */
@Composable
fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(60_000L - (System.currentTimeMillis() % 60_000L))
    }
}

/** App-wide clock style. It is Compose state, so every screen showing a time updates when it changes. */
object TimeFormat {
    var is24h by mutableStateOf(true)
}

private val hhmm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val hmma: DateTimeFormatter get() = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.getDefault())

fun formatMinute(minute: Int): String =
    LocalTime.of((minute / 60).coerceIn(0, 23), minute % 60).format(if (TimeFormat.is24h) hhmm else hmma)

fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute
