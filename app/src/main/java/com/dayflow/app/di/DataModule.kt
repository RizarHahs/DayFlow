package com.dayflow.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.dayflow.app.data.local.*
import com.dayflow.app.data.repo.*
import com.dayflow.app.domain.repo.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): DayflowDatabase =
        Room.databaseBuilder(ctx, DayflowDatabase::class.java, "dayflow.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides fun habitDao(db: DayflowDatabase): HabitDao = db.habitDao()
    @Provides fun taskDao(db: DayflowDatabase): TaskDao = db.taskDao()
    @Provides fun subtaskDao(db: DayflowDatabase): SubtaskDao = db.subtaskDao()
    @Provides fun ideaDao(db: DayflowDatabase): IdeaDao = db.ideaDao()
    @Provides fun roadmapDao(db: DayflowDatabase): RoadmapDao = db.roadmapDao()
    @Provides fun alarmDao(db: DayflowDatabase): AlarmDao = db.alarmDao()
    @Provides fun timerDao(db: DayflowDatabase): TimerDao = db.timerDao()
    @Provides fun historyDao(db: DayflowDatabase): HistoryDao = db.historyDao()
    @Provides fun backupDao(db: DayflowDatabase): BackupDao = db.backupDao()
    @Provides fun scheduledMessageDao(db: DayflowDatabase): ScheduledMessageDao = db.scheduledMessageDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun habits(impl: HabitRepositoryImpl): HabitRepository
    @Binds abstract fun tasks(impl: TaskRepositoryImpl): TaskRepository
    @Binds abstract fun ideas(impl: IdeaRepositoryImpl): IdeaRepository
    @Binds abstract fun roadmap(impl: RoadmapRepositoryImpl): RoadmapRepository
    @Binds abstract fun history(impl: HistoryRepositoryImpl): HistoryRepository
    @Binds abstract fun tx(impl: RoomTransactionRunner): TransactionRunner
    @Binds abstract fun alarmController(impl: com.dayflow.app.platform.alarm.AlarmControllerImpl): AlarmController
    @Binds abstract fun messageController(impl: com.dayflow.app.platform.whatsapp.ScheduledMessageControllerImpl): ScheduledMessageController
    @Binds abstract fun timerController(impl: com.dayflow.app.platform.timer.TimerEngine): TimerController
}

@Singleton
class RoomTransactionRunner @Inject constructor(private val db: DayflowDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}
