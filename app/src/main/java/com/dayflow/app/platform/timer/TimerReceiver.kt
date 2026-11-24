package com.dayflow.app.platform.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayflow.app.platform.Actions
import com.dayflow.app.platform.runAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Finish alarm and notification buttons (Pause / Resume / Stop / Dismiss). Not exported. */
@AndroidEntryPoint
class TimerReceiver : BroadcastReceiver() {
    @Inject lateinit var engine: TimerEngine

    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getLongExtra(Actions.EXTRA_TIMER_ID, -1L)
        when (intent.action) {
            Actions.TIMER_FINISH -> runAsync { engine.onFinishAlarm(id) }
            Actions.TIMER_PAUSE -> runAsync { engine.pause() }
            Actions.TIMER_RESUME -> runAsync { engine.resume() }
            Actions.TIMER_CANCEL -> runAsync { engine.cancel() }
            Actions.TIMER_DISMISS -> runAsync { engine.dismissFinished() }
        }
    }
}
