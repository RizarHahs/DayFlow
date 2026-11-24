package com.dayflow.app.platform.whatsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayflow.app.platform.Actions
import com.dayflow.app.platform.runAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Delivery point of a scheduled message's alarm. Internal only (not exported). */
@AndroidEntryPoint
class ScheduledMessageReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ScheduledMessageScheduler

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Actions.MESSAGE_FIRE) return
        val id = intent.getLongExtra(Actions.EXTRA_MESSAGE_ID, Long.MIN_VALUE)
        if (id == Long.MIN_VALUE) return
        runAsync { scheduler.onFire(id) }
    }
}
