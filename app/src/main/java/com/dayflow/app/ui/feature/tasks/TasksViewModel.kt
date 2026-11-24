package com.dayflow.app.ui.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.repo.TimerController
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.usecase.ConvertUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUi(
    val today: Int,
    val habits: List<Habit>,
    val tasks: List<TaskItem>,
    val upcoming: List<TaskItem>,
) {
    fun lane(kind: TaskKind) = tasks.filter { it.kind == kind }
    fun upcoming(kind: TaskKind) = upcoming.filter { it.kind == kind }
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val habits: HabitRepository,
    private val tasks: TaskRepository,
    private val convert: ConvertUseCase,
    private val timers: TimerController,
    private val clock: DayClock,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<TasksUi?> = clock.todayFlow.flatMapLatest { day ->
        combine(habits.observeHabits(day), tasks.observeToday(day), tasks.observeUpcoming(day)) { h, t, u ->
            TasksUi(day, h, t, u)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun run(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    fun toggleHabit(id: Long) = run { habits.toggleDone(id, clock.today()) }
    fun archiveHabit(id: Long) = run { habits.archive(id, clock.today()) }
    fun reorderHabits(ids: List<Long>) = run { habits.reorder(ids) }

    fun toggleTask(id: Long) = run { tasks.toggleDone(id, clock.today()) }
    fun toggleSubtask(id: Long) = run { tasks.toggleSubtask(id) }
    fun reorderTasks(ids: List<Long>) = run { tasks.reorder(ids) }
    fun moveTo(id: Long, day: Int) = run { tasks.reschedule(id, day) }
    fun moveToToday(id: Long) = run { tasks.reschedule(id, clock.today()) }
    fun moveToTomorrow(id: Long) = run { tasks.reschedule(id, clock.today() + 1) }
    fun drop(id: Long) = run { tasks.drop(id) }
    fun promote(id: Long, kind: TaskKind) = run { convert.taskTo(id, kind) }
    fun toHabit(id: Long) = run { convert.taskToHabit(id) }

    /** Starts the task's own timer length, or its estimate if no timer was attached. */
    fun startTaskTimer(t: TaskItem) = run {
        val ms = t.timerSeconds?.let { it * 1000L } ?: t.estimateMin?.let { it * 60_000L } ?: return@run
        timers.start(t.title, ms, TimerTarget.TASK, t.id)
    }

    fun startHabitTimer(h: Habit) = run {
        val ms = h.timerSeconds?.let { it * 1000L } ?: return@run
        timers.start(h.title, ms, TimerTarget.HABIT, h.id)
    }

    fun addNormal(title: String) = run {
        if (title.isBlank()) return@run
        tasks.save(TaskDraft(kind = TaskKind.NORMAL, title = title, plannedDay = clock.today()))
    }
}
