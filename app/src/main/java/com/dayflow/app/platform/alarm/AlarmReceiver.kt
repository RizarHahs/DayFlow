package com.dayflow.app.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayflow.app.platform.Actions
import com.dayflow.app.platform.runAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Delivery point of AlarmManager. Internal only (not exported). */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Actions.ALARM_FIRE) return
        val id = intent.getLongExtra(Actions.EXTRA_ALARM_ID, Long.MIN_VALUE)
        if (id == Long.MIN_VALUE) return
        val snooze = intent.getBooleanExtra(Actions.EXTRA_SNOOZE_FIRE, false)
        runAsync { scheduler.onFired(id, snooze) }
    }
}
