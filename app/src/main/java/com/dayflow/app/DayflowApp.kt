package com.dayflow.app

import android.app.Application
import com.dayflow.app.platform.Channels
import com.dayflow.app.platform.alarm.AlarmScheduler
import com.dayflow.app.platform.notify.ReminderSync
import com.dayflow.app.platform.timer.TimerEngine
import com.dayflow.app.platform.whatsapp.ScheduledMessageScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DayflowApp : Application() {
    @Inject lateinit var alarms: AlarmScheduler
    @Inject lateinit var timers: TimerEngine
    @Inject lateinit var reminderSync: ReminderSync
    @Inject lateinit var messages: ScheduledMessageScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Channels.ensure(this)
        // Self-heal on every process start: the OS may have dropped alarms (force-stop, update, OEM cleanup).
        appScope.launch {
            runCatching { alarms.rescheduleAll(reportMissed = true) }
            runCatching { timers.restore() }
            runCatching { messages.rescheduleAll() }
            reminderSync.start(appScope)
        }
    }
}
