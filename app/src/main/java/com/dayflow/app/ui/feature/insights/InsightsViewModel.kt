package com.dayflow.app.ui.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.domain.model.DaySummary
import com.dayflow.app.domain.model.HabitHistory
import com.dayflow.app.domain.repo.HistoryRepository
import com.dayflow.app.domain.time.CalendarMath
import com.dayflow.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class InsightsMode(val label: String) { WEEK("Week"), MONTH("Month"), HABITS("Habits") }

data class InsightsUi(
    val mode: InsightsMode,
    val today: Int,
    val weekStart: Int,
    val from: Int,
    val to: Int,
    val anchor: Int,
    val canGoForward: Boolean,
    val summaries: Map<Int, DaySummary>,
    val habits: List<HabitHistory>,
) {
    private val withData get() = summaries.values.filter { it.hasData }
    val averageScorePercent: Int? get() = withData.takeIf { it.isNotEmpty() }?.let { Math.round(it.map { s -> s.score }.average().toFloat() * 100) }
    val completeDays: Int get() = withData.count { it.dayComplete }
    val habitsDone: Int get() = withData.sumOf { it.habitsDone }
    val habitsTotal: Int get() = withData.sumOf { it.habitsTotal }
    val trackedMs: Long get() = withData.sumOf { it.trackedMs }
    val ideas: Int get() = withData.sumOf { it.ideasCaptured }
    val tasksDone: Int get() = withData.sumOf { it.importantDone + it.normalDone + it.extraDone }
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val history: HistoryRepository,
    prefs: AppPrefs,
    clock: DayClock,
) : ViewModel() {

    private val mode = MutableStateFlow(InsightsMode.WEEK)
    private val offset = MutableStateFlow(0)       // 0 = current week/month, -1 = previous, ...

    private data class Params(val mode: InsightsMode, val offset: Int, val weekStart: Int, val today: Int)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<InsightsUi?> = combine(mode, offset, prefs.appearance, clock.todayFlow) { m, o, a, t ->
        Params(m, o, a.weekStart, t)
    }.flatMapLatest { p ->
        when (p.mode) {
            InsightsMode.WEEK -> {
                val start = CalendarMath.startOfWeek(p.today, p.weekStart) + 7 * p.offset
                history.observeSummaries(start, start + 6).map { list ->
                    InsightsUi(p.mode, p.today, p.weekStart, start, start + 6, start, p.offset < 0, list.associateBy { it.day }, emptyList())
                }
            }
            InsightsMode.MONTH -> {
                val anchor = CalendarMath.shiftMonth(p.today, p.offset)
                val from = CalendarMath.monthStart(anchor)
                val to = CalendarMath.monthEnd(anchor)
                history.observeSummaries(from, to).map { list ->
                    InsightsUi(p.mode, p.today, p.weekStart, from, to, anchor, p.offset < 0, list.associateBy { it.day }, emptyList())
                }
            }
            InsightsMode.HABITS -> history.observeHabitHistory(p.today).map { h ->
                InsightsUi(p.mode, p.today, p.weekStart, p.today, p.today, p.today, false, emptyMap(), h)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentMode: StateFlow<InsightsMode> = mode

    fun setMode(m: InsightsMode) { mode.value = m; offset.value = 0 }
    fun back() { offset.value = offset.value - 1 }
    fun forward() { offset.value = minOf(0, offset.value + 1) }
    fun thisPeriod() { offset.value = 0 }
}
