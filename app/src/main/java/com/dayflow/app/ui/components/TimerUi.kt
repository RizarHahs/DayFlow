package com.dayflow.app.ui.components

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dayflow.app.domain.model.TimerSession
import com.dayflow.app.domain.model.TimerState
import kotlinx.coroutines.delay

/**
 * Display-only countdown. The truth lives in the persisted session; this just re-reads the clock
 * (monotonic within a boot, wall clock after a reboot) five times a second while RUNNING.
 */
@Composable
fun rememberRemainingMs(session: TimerSession?, bootCount: Int): Long {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(session?.id, session?.state) {
        while (session?.state == TimerState.RUNNING) {
            tick = SystemClock.elapsedRealtime()
            delay(200)
        }
    }
    @Suppress("UNUSED_VARIABLE") val observeTick = tick   // reading it is what triggers recomposition
    return session?.remainingMs(SystemClock.elapsedRealtime(), System.currentTimeMillis(), bootCount) ?: 0L
}

/** 1:05, 25:00, 1:00:00 (rounds up so it never shows 0:00 while time remains). */
fun formatDuration(ms: Long): String {
    val total = (ms + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
