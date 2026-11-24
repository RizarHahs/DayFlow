package com.dayflow.app.platform.notify

import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.RoadmapRepository
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.time.DayClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the reminder alarm correct whenever habits, tasks, roadmap or notification settings change. */
@Singleton
class ReminderSync @Inject constructor(
    private val habits: HabitRepository,
    private val tasks: TaskRepository,
    private val roadmap: RoadmapRepository,
    private val prefs: AppPrefs,
    private val clock: DayClock,
    private val scheduler: ReminderScheduler,
) {
    private var started = false

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        scope.launch {
            clock.todayFlow.flatMapLatest { day ->
                val sources: List<Flow<Unit>> = listOf(
                    habits.observeHabits(day).map { },
                    tasks.observeToday(day).map { },
                    tasks.observeUpcoming(day).map { },
                    roadmap.observeItems().map { },
                    roadmap.observeDayLogs(day).map { },
                    prefs.notifications.map { },
                )
                combine(sources) { }
            }.debounce(500).collect { runCatching { scheduler.reschedule() } }
        }
    }
}
