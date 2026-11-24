package com.dayflow.app.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object CalendarMath {
    /** [weekStart] is a java.time DayOfWeek value (1 = Monday ... 7 = Sunday). */
    fun startOfWeek(day: Int, weekStart: Int): Int {
        val dow = day.toLocalDate().dayOfWeek.value          // 1..7
        val back = ((dow - weekStart) % 7 + 7) % 7
        return day - back
    }

    fun monthStart(day: Int): Int = day.toLocalDate().withDayOfMonth(1).toEpochDay().toInt()
    fun monthEnd(day: Int): Int = day.toLocalDate().let { it.withDayOfMonth(it.lengthOfMonth()) }.toEpochDay().toInt()
    fun shiftMonth(day: Int, delta: Int): Int = day.toLocalDate().withDayOfMonth(1).plusMonths(delta.toLong()).toEpochDay().toInt()

    /** Full weeks covering the month; entries outside the month are null. Size is a multiple of 7. */
    fun monthGrid(anyDayInMonth: Int, weekStart: Int): List<Int?> {
        val first = monthStart(anyDayInMonth)
        val last = monthEnd(anyDayInMonth)
        val lead = first - startOfWeek(first, weekStart)
        val cells = mutableListOf<Int?>()
        repeat(lead) { cells += null }
        for (d in first..last) cells += d
        while (cells.size % 7 != 0) cells += null
        return cells
    }

    fun weekdayLabels(weekStart: Int, locale: Locale = Locale.getDefault()): List<String> =
        (0 until 7).map { DayOfWeek.of((weekStart - 1 + it) % 7 + 1).getDisplayName(TextStyle.SHORT, locale) }

    fun monthTitle(day: Int, locale: Locale = Locale.getDefault()): String {
        val d: LocalDate = day.toLocalDate()
        return d.month.getDisplayName(TextStyle.FULL, locale) + " " + d.year
    }
}
