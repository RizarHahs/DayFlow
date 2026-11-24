package com.dayflow.app.domain.time

import com.dayflow.app.domain.model.ALL_DAYS
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Int.toLocalDate(): LocalDate = LocalDate.ofEpochDay(toLong())

/** Monday = 0 ... Sunday = 6 */
fun weekdayIndex(day: Int): Int = day.toLocalDate().dayOfWeek.value - 1

fun isScheduled(mask: Int, day: Int): Boolean = (mask shr weekdayIndex(day)) and 1 == 1

private val short: Array<String> = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun maskLabel(mask: Int): String = when (mask and ALL_DAYS) {
    ALL_DAYS -> "Every day"
    0b0011111 -> "Weekdays"
    0b1100000 -> "Weekends"
    0 -> "Never"
    else -> short.filterIndexed { i, _ -> (mask shr i) and 1 == 1 }.joinToString(" ")
}

private val dayFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

fun dayLabel(day: Int, today: Int): String = when (day - today) {
    0 -> "Today"
    1 -> "Tomorrow"
    -1 -> "Yesterday"
    else -> day.toLocalDate().format(dayFmt)
}

fun dayLongLabel(day: Int): String =
    day.toLocalDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))

@Suppress("unused")
val weekStart: DayOfWeek = DayOfWeek.MONDAY
