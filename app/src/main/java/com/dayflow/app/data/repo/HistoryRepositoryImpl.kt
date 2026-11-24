package com.dayflow.app.data.repo

import com.dayflow.app.data.local.*
import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.repo.HistoryRepository
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.time.toLocalDate
import com.dayflow.app.domain.usecase.HabitStatsCalc
import com.dayflow.app.domain.usecase.SummaryBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val taskDao: TaskDao,
    private val ideaDao: IdeaDao,
    private val timerDao: TimerDao,
    private val historyDao: HistoryDao,
    private val prefs: AppPrefs,
    private val clock: DayClock,
) : HistoryRepository {

    private fun startOf(day: Int): Long = day.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun HabitEntity.toDef() = HabitDef(id, title, repeatMask, createdDay, archivedDay)
    private fun TaskEntity.toLite() = TaskLite(kind, title, status == TaskStatus.DONE)

    private data class Core(
        val habits: List<HabitEntity>, val doneIds: List<Long>, val tasks: List<TaskEntity>,
        val ideas: List<IdeaEntity>, val focusMs: Long,
    )

    override fun observeDetail(day: Int): Flow<DayDetail> {
        val from = startOf(day)
        val to = startOf(day + 1)
        val core = combine(
            habitDao.observeAllIncludingArchived(), habitDao.observeDoneIds(day), taskDao.observeToday(day),
            ideaDao.observeCreatedBetween(from, to), timerDao.observeFocusBetween(from, to),
        ) { h, d, t, i, f -> Core(h, d, t, i, f) }
        return core.combine(historyDao.observeNote(day)) { c, note -> c to note?.text.orEmpty() }
            .combine(historyDao.observeRange(day, day)) { (c, note), stored ->
                val built = SummaryBuilder.build(
                    day, c.habits.map { it.toDef() }, c.doneIds.toSet(), c.tasks.map { it.toLite() },
                    c.ideas.map { it.text }, c.focusMs, note,
                )
                // Past days show the snapshot taken at the time; today is always live.
                val snap = stored.firstOrNull()?.takeIf { day < clock.today() }
                if (snap == null) built else built.copy(summary = snap.toDomain())
            }
    }

    override fun observeSummaries(from: Int, to: Int): Flow<List<DaySummary>> {
        val today = clock.today()
        val stored = historyDao.observeRange(from, minOf(to, today - 1)).map { l -> l.map { it.toDomain() } }
        if (today !in from..to) return stored
        val live = observeDetail(today).map { it.summary }
        return combine(stored, live) { s, l -> s + l }
    }

    override fun observeHabitHistory(today: Int): Flow<List<HabitHistory>> =
        combine(habitDao.observeAllIncludingArchived(), habitDao.observeLogs()) { habits, logs ->
            val byHabit = logs.groupBy { it.habitId }
            habits
                .filter { it.createdDay <= today || byHabit.containsKey(it.id) }
                .sortedWith(compareBy<HabitEntity> { it.archivedDay != null }.thenBy { it.sortOrder })
                .filter { it.archivedDay == null || byHabit.containsKey(it.id) }
                .map { h ->
                    val def = h.toDef()
                    val done = byHabit[h.id]?.map { it.day }?.toSet().orEmpty()
                    HabitHistory(h.id, h.title, h.archivedDay != null, HabitStatsCalc.compute(def, done, today), HabitStatsCalc.dots(def, done, today))
                }
        }

    override suspend fun finalizeBefore(today: Int) {
        val last = prefs.lastFinalizedDay()
        val first = last?.plus(1) ?: earliestDataDay() ?: run { prefs.setLastFinalizedDay(today - 1); return }
        val start = maxOf(first, today - MAX_BACKFILL_DAYS)
        for (d in start until today) {
            val detail = compute(d)
            if (detail.summary.hasData) historyDao.upsertSummary(detail.summary.toEntity())
        }
        prefs.setLastFinalizedDay(today - 1)
    }

    private suspend fun earliestDataDay(): Int? {
        val candidates = listOfNotNull(
            habitDao.getAllIncludingArchived().minOfOrNull { it.createdDay },
            taskDao.earliestPlannedDay(),
            ideaDao.earliestCreatedAt()?.let {
                java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay().toInt()
            },
        )
        return candidates.minOrNull()
    }

    private suspend fun compute(day: Int): DayDetail {
        val from = startOf(day)
        val to = startOf(day + 1)
        return SummaryBuilder.build(
            day,
            habitDao.getAllIncludingArchived().map { it.toDef() },
            habitDao.doneIds(day).toSet(),
            taskDao.forDay(day).map { it.toLite() },
            ideaDao.createdBetween(from, to).map { it.text },
            timerDao.focusBetween(from, to),
            note = "",
        )
    }

    override suspend fun saveNote(day: Int, text: String) {
        if (text.isBlank()) historyDao.deleteNote(day)
        else historyDao.upsertNote(DayNoteEntity(day, text.trim(), System.currentTimeMillis()))
    }

    private fun DaySummaryEntity.toDomain() = DaySummary(
        day, habitsDone, habitsTotal, importantDone, importantTotal, normalDone, normalTotal,
        extraDone, extraTotal, score, trackedMs, ideasCaptured, dayComplete,
    )

    private fun DaySummary.toEntity() = DaySummaryEntity(
        day, habitsDone, habitsTotal, importantDone, importantTotal, normalDone, normalTotal,
        extraDone, extraTotal, score, trackedMs, ideasCaptured, dayComplete, System.currentTimeMillis(),
    )

    companion object { private const val MAX_BACKFILL_DAYS = 120 }
}
