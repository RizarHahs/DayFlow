package com.dayflow.app.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

data class Tick(val day: Int, val minute: Int)

/**
 * Single source of "now". Days are epoch-days, times are minute-of-day, both in the device's
 * *current* zone, so a timezone change is picked up on the next tick. (Phase 4 adds broadcast triggers.)
 */
@Singleton
class DayClock @Inject constructor() {
    fun today(): Int = LocalDate.now().toEpochDay().toInt()
    fun minuteOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    /** Emits immediately, then at every minute boundary. */
    val ticks: Flow<Tick> = flow {
        while (true) {
            emit(Tick(today(), minuteOfDay()))
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }.distinctUntilChanged()

    val todayFlow: Flow<Int> = ticks.map { it.day }.distinctUntilChanged()
}
