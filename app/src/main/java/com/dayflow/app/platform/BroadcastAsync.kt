package com.dayflow.app.platform

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Lets a receiver do suspend work (DB, rescheduling) without the process being killed mid-way:
 * goAsync() keeps the broadcast alive for up to ~10 s, which is plenty for these operations.
 */
fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try { block() } catch (_: Throwable) { /* never crash the process from a receiver */ } finally { pending.finish() }
    }
}
