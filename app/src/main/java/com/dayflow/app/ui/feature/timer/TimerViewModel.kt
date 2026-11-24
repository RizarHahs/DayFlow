package com.dayflow.app.ui.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.RoadmapItem
import com.dayflow.app.domain.model.TaskItem
import com.dayflow.app.domain.model.TimerSession
import com.dayflow.app.domain.model.TimerTarget
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.RoadmapRepository
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.repo.TimerController
import com.dayflow.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Activity-scoped (created at the root) so the mini-bar and the full screen share one source. */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timers: TimerController,
    habits: HabitRepository,
    tasks: TaskRepository,
    roadmap: RoadmapRepository,
    clock: DayClock,
    prefs: AppPrefs,
) : ViewModel() {

    val defaultMinutes: StateFlow<Int> = prefs.appearance.map { it.defaultTimerMin }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 25)


    val current: StateFlow<TimerSession?> = timers.current
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bootCount: Int get() = timers.bootCount()

    val habitOptions: StateFlow<List<Habit>> = habits.observeHabits(clock.today())
        .map { l -> l.filter { it.dueToday } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val taskOptions: StateFlow<List<TaskItem>> = tasks.observeToday(clock.today())
        .map { l -> l.filter { !it.done } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activityOptions: StateFlow<List<RoadmapItem>> = roadmap.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    fun start(label: String, minutes: Int, target: TimerTarget?, targetId: Long?) =
        launch { timers.start(label, minutes * 60_000L, target, targetId) }
    fun pause() = launch { timers.pause() }
    fun resume() = launch { timers.resume() }
    fun cancel() = launch { timers.cancel() }
    fun reset() = launch { timers.reset() }
    fun dismiss() = launch { timers.dismissFinished() }
}
