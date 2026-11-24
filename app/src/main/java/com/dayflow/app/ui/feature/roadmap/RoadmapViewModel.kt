package com.dayflow.app.ui.feature.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.RoadmapSlot
import com.dayflow.app.domain.model.SlotStatus
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.RoadmapRepository
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.repo.TimerController
import com.dayflow.app.domain.model.TimerTarget
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.usecase.Conflicts
import com.dayflow.app.domain.usecase.RoadmapSlots
import com.dayflow.app.domain.usecase.TimeBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoadmapUi(val day: Int, val today: Int, val nowMinute: Int, val slots: List<RoadmapSlot>) {
    val isToday get() = day == today
}

@HiltViewModel
class RoadmapViewModel @Inject constructor(
    private val roadmap: RoadmapRepository,
    private val habits: HabitRepository,
    private val tasks: TaskRepository,
    private val timers: TimerController,
    private val clock: DayClock,
) : ViewModel() {

    /** null = follow today (so the screen rolls over at midnight on its own). */
    private val selected = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<RoadmapUi?> = combine(selected, clock.ticks) { sel, tick -> (sel ?: tick.day) to tick }
        .flatMapLatest { (day, tick) ->
            combine(
                roadmap.observeItems(), roadmap.observeDayLogs(day),
                habits.observeHabits(day), tasks.observeToday(day),
            ) { items, logs, hs, ts ->
                val now = when {
                    day < tick.day -> 24 * 60   // past: anything unfinished is missed
                    day > tick.day -> -1        // future: everything upcoming
                    else -> tick.minute
                }
                RoadmapUi(
                    day, tick.day, tick.minute,
                    RoadmapSlots.build(
                        day, now, items, logs,
                        hs.filter { it.done }.map { it.id }.toSet(),
                        ts.filter { it.done }.map { it.id }.toSet(),
                    ),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun shiftDay(delta: Int) {
        val cur = ui.value?.day ?: clock.today()
        val target = cur + delta
        selected.value = if (target == clock.today()) null else target
    }
    fun goToday() { selected.value = null }

    fun toggleDone(s: RoadmapSlot) = launch {
        val day = ui.value?.day ?: return@launch
        roadmap.setDone(s.itemId, day, done = s.status != SlotStatus.DONE)
    }
    fun skip(s: RoadmapSlot) = launch { ui.value?.day?.let { roadmap.skip(s.itemId, it) } }
    fun restoreOriginal(s: RoadmapSlot) = launch { ui.value?.day?.let { roadmap.clearDay(s.itemId, it) } }
    fun reschedule(s: RoadmapSlot, start: Int, end: Int) = launch { ui.value?.day?.let { roadmap.reschedule(s.itemId, it, start, end) } }
    fun deleteActivity(s: RoadmapSlot) = launch { roadmap.delete(s.itemId) }

    /** Timer for the activity's own length, started straight from the timeline. */
    fun startTimer(s: RoadmapSlot) = launch {
        timers.start(s.title, (s.endMinute - s.startMinute).coerceAtLeast(1) * 60_000L, TimerTarget.ACTIVITY, s.itemId)
    }

    /** Overlaps a time change would create on the selected day (user decides whether to proceed). */
    fun conflictsFor(s: RoadmapSlot, start: Int, end: Int): List<TimeBlock> =
        Conflicts.forSlot(s.itemId, start, end, ui.value?.slots.orEmpty())

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
