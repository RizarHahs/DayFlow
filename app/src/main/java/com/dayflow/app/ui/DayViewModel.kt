package com.dayflow.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.HistoryRepository
import com.dayflow.app.domain.repo.IdeaRepository
import com.dayflow.app.domain.repo.RoadmapRepository
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.usecase.DayScoring
import com.dayflow.app.domain.usecase.RolloverUseCase
import com.dayflow.app.domain.usecase.RoadmapSlots
import com.dayflow.app.platform.alarm.AlarmScheduler
import com.dayflow.app.platform.whatsapp.ScheduledMessageScheduler
import com.dayflow.app.platform.setup.SetupChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayUiState(
    val habits: List<Habit>,        // due today only
    val important: List<TaskItem>,
    val normal: List<TaskItem>,
    val extra: List<TaskItem>,
    val ideas: List<Idea>,          // not archived
    val roadmap: List<RoadmapEntry>,
) {
    val foundation get() = CategoryProgress(habits.count { it.done }, habits.size)
    val importantProgress get() = CategoryProgress(important.count { it.done }, important.size)
    val normalProgress get() = CategoryProgress(normal.count { it.done }, normal.size)
    val extraProgress get() = CategoryProgress(extra.count { it.done }, extra.size)
    val score get() = DayScoring.weighted(foundation, importantProgress, normalProgress)
    val dayComplete get() = DayScoring.dayComplete(foundation, importantProgress, normalProgress)
}

/** Owns "today" for the Home screen: observes Room, runs the new-day rollover, and handles quick-add. */
@HiltViewModel
class DayViewModel @Inject constructor(
    private val habits: HabitRepository,
    private val tasks: TaskRepository,
    private val ideas: IdeaRepository,
    private val roadmap: RoadmapRepository,
    private val history: HistoryRepository,
    private val rollover: RolloverUseCase,
    private val clock: DayClock,
    private val setup: SetupChecker,
    private val alarmScheduler: AlarmScheduler,
    private val messageScheduler: ScheduledMessageScheduler,
) : ViewModel() {

    private val _attention = MutableStateFlow(!setup.check().alarmsReliable)
    /** True when alarm permissions are missing: shows the amber dot on Home. */
    val setupAttention: StateFlow<Boolean> = _attention.asStateFlow()
    /** Called on every resume: re-reads permissions and re-arms alarms (e.g. right after exact-alarm access is granted). */
    fun todayNow(): Int = clock.today()

    fun refreshSetup() {
        _attention.value = !setup.check().alarmsReliable
        viewModelScope.launch {
            runCatching { alarmScheduler.rescheduleAll() }
            runCatching { messageScheduler.rescheduleAll() }
        }
    }

    init {
        // Runs at startup and again whenever the local date changes while the app is alive.
        // Order matters: snapshot the finished day first, then carry unfinished tasks forward.
        viewModelScope.launch {
            clock.todayFlow.collect {
                runCatching { history.finalizeBefore(it) }
                rollover(it)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DayUiState?> = clock.todayFlow.flatMapLatest { day ->
        combine(
            habits.observeHabits(day), tasks.observeToday(day), ideas.observeAll(),
            roadmap.observeItems(), roadmap.observeDayLogs(day),
        ) { h, t, i, items, logs ->
            DayUiState(
                habits = h.filter { it.dueToday },
                important = t.filter { it.kind == TaskKind.IMPORTANT },
                normal = t.filter { it.kind == TaskKind.NORMAL },
                extra = t.filter { it.kind == TaskKind.EXTRA },
                ideas = i.filter { !it.archived },
                roadmap = RoadmapSlots.build(day, -1, items, logs, emptySet(), emptySet())
                    .filter { it.status != SlotStatus.SKIPPED }
                    .map { RoadmapEntry(it.itemId, it.title, it.startMinute, it.endMinute) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    fun toggleHabit(id: Long) = launch { habits.toggleDone(id, clock.today()) }
    fun toggleTask(id: Long) = launch { tasks.toggleDone(id, clock.today()) }
    fun togglePin(id: Long) = launch { ideas.togglePin(id) }

    fun add(item: NewItem) = launch {
        val title = item.title.trim()
        if (title.isEmpty()) return@launch
        val today = clock.today()
        when (item.kind) {
            ItemKind.HABIT -> habits.save(HabitDraft(title = title, period = item.period))
            ItemKind.IMPORTANT -> tasks.save(
                TaskDraft(kind = TaskKind.IMPORTANT, title = title, priority = item.priority, plannedDay = today)
            )
            ItemKind.NORMAL -> tasks.save(TaskDraft(kind = TaskKind.NORMAL, title = title, plannedDay = today))
            ItemKind.EXTRA -> tasks.save(TaskDraft(kind = TaskKind.EXTRA, title = title, plannedDay = today))
            ItemKind.IDEA -> ideas.save(IdeaDraft(text = title))
        }
    }
}
