package com.dayflow.app.platform.notify

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.platform.Actions
import com.dayflow.app.platform.runAsync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var habits: HabitRepository
    @Inject lateinit var tasks: TaskRepository

    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {
            Actions.REMINDER_FIRE -> runAsync { scheduler.onFire() }
            Actions.REMINDER_DONE -> {
                val kind = intent.getStringExtra(Actions.EXTRA_KIND) ?: return
                val id = intent.getLongExtra(Actions.EXTRA_TARGET_ID, -1)
                val day = intent.getIntExtra(Actions.EXTRA_DAY, 0)
                runAsync {
                    // Only ever marks DONE (never toggles back), so a double tap cannot undo it.
                    if (kind == "H") {
                        habits.observeHabits(day).first().firstOrNull { it.id == id }
                            ?.takeIf { !it.done }?.let { habits.toggleDone(id, day) }
                    } else {
                        tasks.getItem(id)?.takeIf { !it.done }?.let { tasks.toggleDone(id, day) }
                    }
                    context.getSystemService<NotificationManager>()
                        ?.cancel(ReminderScheduler.notificationId("$kind:$id:$day"))
                }
            }
        }
    }
}
