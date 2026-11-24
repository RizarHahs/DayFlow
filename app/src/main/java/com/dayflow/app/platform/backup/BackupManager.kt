package com.dayflow.app.platform.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.dayflow.app.data.local.*
import com.dayflow.app.domain.repo.TimerController
import com.dayflow.app.domain.time.toLocalDate
import com.dayflow.app.platform.alarm.AlarmScheduler
import com.dayflow.app.platform.notify.ReminderScheduler
import com.dayflow.app.platform.whatsapp.ScheduledMessageScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** The whole personal database as one JSON document. Timers and preferences are deliberately not included. */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val dbVersion: Int,
    val exportedAt: Long,
    val habits: List<HabitEntity>,
    val habitLogs: List<HabitLogEntity>,
    val tasks: List<TaskEntity>,
    val subtasks: List<SubtaskEntity>,
    val ideas: List<IdeaEntity>,
    val roadmapItems: List<RoadmapItemEntity>,
    val roadmapDays: List<RoadmapDayEntity>,
    val alarms: List<AlarmEntity>,
    val daySummaries: List<DaySummaryEntity>,
    val dayNotes: List<DayNoteEntity>,
    /** null = a backup made before this feature existed: restoring it leaves current scheduled messages untouched. */
    val scheduledMessages: List<ScheduledMessageEntity>? = null,
) {
    companion object {
        const val FORMAT = "dayflow-backup"
        const val VERSION = 1
    }
}

sealed interface BackupResult {
    data class Ok(val message: String) : BackupResult
    data class Failed(val reason: String) : BackupResult
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: DayflowDatabase,
    private val dao: BackupDao,
    private val timers: TimerController,
    private val alarms: AlarmScheduler,
    private val reminders: ReminderScheduler,
    private val messages: ScheduledMessageScheduler,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    // ---------- backup / restore ----------

    suspend fun backupTo(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val file = BackupFile(
                dbVersion = 4, exportedAt = System.currentTimeMillis(),
                habits = dao.habits(), habitLogs = dao.habitLogs(), tasks = dao.tasks(), subtasks = dao.subtasks(),
                ideas = dao.ideas(), roadmapItems = dao.roadmapItems(), roadmapDays = dao.roadmapDays(),
                alarms = dao.alarms(), daySummaries = dao.daySummaries(), dayNotes = dao.dayNotes(),
                scheduledMessages = dao.scheduledMessages(),
            )
            write(uri, json.encodeToString(BackupFile.serializer(), file))
            BackupResult.Ok("Backup saved: ${file.habits.size} habits, ${file.tasks.size} tasks, ${file.ideas.size} ideas, ${file.alarms.size} alarms.")
        } catch (e: Exception) {
            BackupResult.Failed("Couldn't write the backup (${e.message ?: "unknown error"}).")
        }
    }

    /**
     * Validates first, then replaces everything in ONE transaction: a bad or unreadable file changes nothing.
     */
    suspend fun restoreFrom(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), read(uri))
        } catch (e: Exception) {
            return@withContext BackupResult.Failed("That doesn't look like a DAYFLOW backup.")
        }
        if (file.format != BackupFile.FORMAT) return@withContext BackupResult.Failed("That doesn't look like a DAYFLOW backup.")
        if (file.version > BackupFile.VERSION) return@withContext BackupResult.Failed("This backup was made by a newer version of DAYFLOW. Update the app first.")
        try {
            timers.cancel()
            db.withTransaction {
                dao.clearSubtasks(); dao.clearTasks(); dao.clearHabitLogs(); dao.clearHabits(); dao.clearIdeas()
                dao.clearRoadmapDays(); dao.clearRoadmapItems(); dao.clearAlarms()
                dao.clearDaySummaries(); dao.clearDayNotes(); dao.clearTimers()
                dao.putHabits(file.habits); dao.putHabitLogs(file.habitLogs)
                dao.putTasks(file.tasks); dao.putSubtasks(file.subtasks)
                dao.putIdeas(file.ideas)
                dao.putRoadmapItems(file.roadmapItems); dao.putRoadmapDays(file.roadmapDays)
                dao.putAlarms(file.alarms)
                dao.putDaySummaries(file.daySummaries); dao.putDayNotes(file.dayNotes)
                file.scheduledMessages?.let { dao.clearScheduledMessages(); dao.putScheduledMessages(it) }
            }
            // The OS forgot nothing, but our schedule must match the restored data.
            alarms.rescheduleAll()
            reminders.reschedule()
            messages.rescheduleAll()
            BackupResult.Ok("Restored ${file.habits.size} habits, ${file.tasks.size} tasks, ${file.ideas.size} ideas, ${file.alarms.size} alarms.")
        } catch (e: Exception) {
            BackupResult.Failed("Restore failed and nothing was changed (${e.message ?: "unknown error"}).")
        }
    }

    // ---------- exports ----------

    suspend fun exportHabitHistoryCsv(uri: Uri): BackupResult = exportText(uri) {
        val habits = dao.habits().associateBy { it.id }
        val sb = StringBuilder("date,habit,completed\n")
        dao.habitLogs().sortedBy { it.day }.forEach { l ->
            sb.append(l.day.toLocalDate()).append(',').append(csv(habits[l.habitId]?.title ?: "(deleted)")).append(",yes\n")
        }
        sb.toString() to "habit history"
    }

    suspend fun exportTasksCsv(uri: Uri): BackupResult = exportText(uri) {
        val sb = StringBuilder("kind,title,status,planned_date,completed_date,priority,estimate_min,notes\n")
        dao.tasks().sortedBy { it.plannedDay }.forEach { t ->
            sb.append(t.kind.name.lowercase()).append(',').append(csv(t.title)).append(',').append(t.status.name.lowercase()).append(',')
                .append(t.plannedDay.toLocalDate()).append(',').append(t.completedDay?.toLocalDate() ?: "").append(',')
                .append(t.priority?.name?.lowercase() ?: "").append(',').append(t.estimateMin ?: "").append(',')
                .append(csv(t.description)).append('\n')
        }
        sb.toString() to "tasks"
    }

    suspend fun exportIdeasMarkdown(uri: Uri): BackupResult = exportText(uri) {
        val zone = ZoneId.systemDefault()
        val sb = StringBuilder("# DAYFLOW ideas\n\n")
        dao.ideas().sortedByDescending { it.createdAt }.forEach { i ->
            val date = Instant.ofEpochMilli(i.createdAt).atZone(zone).toLocalDate()
            sb.append("## ").append(date).append(if (i.pinned) "  (pinned)" else "").append(if (i.archived) "  (archived)" else "").append("\n\n")
            sb.append(i.text).append("\n\n")
            if (i.description.isNotBlank()) sb.append(i.description).append("\n\n")
            if (i.tags.isNotBlank()) sb.append(i.tags.split(' ').filter { it.isNotBlank() }.joinToString(" ") { "#$it" }).append("\n\n")
        }
        sb.toString() to "ideas"
    }

    private suspend fun exportText(uri: Uri, build: suspend () -> Pair<String, String>): BackupResult = withContext(Dispatchers.IO) {
        try {
            val (text, what) = build()
            write(uri, text)
            BackupResult.Ok("Exported $what.")
        } catch (e: Exception) {
            BackupResult.Failed("Export failed (${e.message ?: "unknown error"}).")
        }
    }

    // ---------- plumbing ----------

    private fun write(uri: Uri, text: String) {
        ctx.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) }
            ?: error("Could not open the file for writing")
    }

    private fun read(uri: Uri): String =
        ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not open the file for reading")

    /** RFC 4180: quote when needed, double the quotes. Also blocks spreadsheet formula injection. */
    private fun csv(raw: String): String {
        val safe = if (raw.firstOrNull() in listOf('=', '+', '-', '@')) "'$raw" else raw
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
