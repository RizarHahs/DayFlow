package com.dayflow.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 = Phase 3. v2 = Phase 4 (alarm, timer_session). v3 = Phase 5 (day_summary, day_note). v4 = scheduled_message. Migrations are
 * written by hand so they work without pre-exported schemas. No destructive fallback: data is never wiped.
 */
@Database(
    entities = [
        HabitEntity::class, HabitLogEntity::class,
        TaskEntity::class, SubtaskEntity::class,
        IdeaEntity::class,
        RoadmapItemEntity::class, RoadmapDayEntity::class,
        AlarmEntity::class, TimerSessionEntity::class,
        DaySummaryEntity::class, DayNoteEntity::class,
        ScheduledMessageEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class DayflowDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun ideaDao(): IdeaDao
    abstract fun roadmapDao(): RoadmapDao
    abstract fun alarmDao(): AlarmDao
    abstract fun timerDao(): TimerDao
    abstract fun historyDao(): HistoryDao
    abstract fun backupDao(): BackupDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
}

/** SQL must match the entity definitions exactly (types, NOT NULL, no defaults) or Room rejects the schema. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `alarm` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`minute` INTEGER NOT NULL, `repeatMask` INTEGER NOT NULL, `label` TEXT NOT NULL, " +
                "`enabled` INTEGER NOT NULL, `kind` TEXT NOT NULL, `soundUri` TEXT, `vibrate` INTEGER NOT NULL, " +
                "`snoozeMin` INTEGER NOT NULL, `linkedHabitId` INTEGER, `linkedRoadmapId` INTEGER, `nextTriggerAt` INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `timer_session` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`label` TEXT NOT NULL, `targetType` TEXT, `targetId` INTEGER, `durationMs` INTEGER NOT NULL, " +
                "`state` TEXT NOT NULL, `startedElapsed` INTEGER NOT NULL, `endElapsed` INTEGER NOT NULL, " +
                "`endEpoch` INTEGER NOT NULL, `remainingMsPaused` INTEGER NOT NULL, `bootCount` INTEGER NOT NULL, " +
                "`startedAt` INTEGER NOT NULL, `focusMs` INTEGER NOT NULL)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_summary` (`day` INTEGER NOT NULL, `habitsDone` INTEGER NOT NULL, " +
                "`habitsTotal` INTEGER NOT NULL, `importantDone` INTEGER NOT NULL, `importantTotal` INTEGER NOT NULL, " +
                "`normalDone` INTEGER NOT NULL, `normalTotal` INTEGER NOT NULL, `extraDone` INTEGER NOT NULL, " +
                "`extraTotal` INTEGER NOT NULL, `score` REAL NOT NULL, `trackedMs` INTEGER NOT NULL, " +
                "`ideasCaptured` INTEGER NOT NULL, `dayComplete` INTEGER NOT NULL, `finalizedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`day`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_note` (`day` INTEGER NOT NULL, `text` TEXT NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`day`))"
        )
    }
}

/**
 * v3 -> v4: purely additive. Creates the new table; touches no existing table or row, so all habits,
 * tasks, ideas, alarms, history and settings are preserved byte for byte.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `scheduled_message` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recipientName` TEXT NOT NULL, `phone` TEXT NOT NULL, `occasion` TEXT NOT NULL, " +
                "`message` TEXT NOT NULL, `day` INTEGER NOT NULL, `minute` INTEGER NOT NULL, " +
                "`status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `triggeredAt` INTEGER, " +
                "`openedAt` INTEGER, `failureReason` TEXT)"
        )
    }
}
