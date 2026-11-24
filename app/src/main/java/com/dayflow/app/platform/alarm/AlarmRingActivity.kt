package com.dayflow.app.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dayflow.app.platform.Actions
import com.dayflow.app.platform.whatsapp.OpenWhatsAppActivity
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DayflowTheme
import com.dayflow.app.ui.theme.DfType

/** Full-screen ringing UI shown over the lock screen. Pure UI: the service owns sound and state. */
class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        val label = intent.getStringExtra(Actions.EXTRA_LABEL) ?: "Alarm"
        val time = intent.getStringExtra(EXTRA_TIME) ?: ""
        val snooze = intent.getIntExtra(Actions.EXTRA_SNOOZE_MIN, 9)
        val messageId = intent.getLongExtra(Actions.EXTRA_MESSAGE_ID, -1L)
        val sub = intent.getStringExtra(Actions.EXTRA_SUBTEXT) ?: ""
        val body = intent.getStringExtra(Actions.EXTRA_TEXT) ?: ""

        setContent {
            DayflowTheme(darkTheme = true) {
                // Close ourselves when the service stops (dismissed from the notification, timeout, ...).
                DisposableEffect(Unit) {
                    val r = object : BroadcastReceiver() {
                        override fun onReceive(c: Context?, i: Intent?) { finish() }
                    }
                    ContextCompat.registerReceiver(
                        this@AlarmRingActivity, r, IntentFilter(Actions.ALARM_STOPPED), ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                    onDispose { unregisterReceiver(r) }
                }
                if (messageId >= 0) {
                    MessageRingScreen(
                        title = label, sub = sub, body = body,
                        onOpen = { startActivity(OpenWhatsAppActivity.intent(this, messageId)); finish() },
                        onDismiss = { startService(AlarmRingService.dismissMessageIntent(this, messageId)); finish() },
                    )
                } else {
                    RingScreen(
                        label = label, time = time, snooze = snooze,
                        onSnooze = { startService(AlarmRingService.actionIntent(this, Actions.ALARM_SNOOZE)); finish() },
                        onDismiss = { startService(AlarmRingService.actionIntent(this, Actions.ALARM_DISMISS)); finish() },
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_TIME = "time"

        fun messageIntent(ctx: Context, messageId: Long, title: String, sub: String, body: String) =
            Intent(ctx, AlarmRingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(Actions.EXTRA_MESSAGE_ID, messageId)
                .putExtra(Actions.EXTRA_LABEL, title)
                .putExtra(Actions.EXTRA_SUBTEXT, sub)
                .putExtra(Actions.EXTRA_TEXT, body)
        fun intent(ctx: Context, label: String, time: String, snoozeMin: Int) =
            Intent(ctx, AlarmRingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(Actions.EXTRA_LABEL, label)
                .putExtra(EXTRA_TIME, time)
                .putExtra(Actions.EXTRA_SNOOZE_MIN, snoozeMin)
    }
}

@Composable
private fun RingScreen(label: String, time: String, snooze: Int, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Df.colors.background).systemBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(time, style = DfType.monoLarge.copy(fontSize = 72.sp, lineHeight = 80.sp), color = Df.colors.textPrimary)
        Spacer(Modifier.height(12.dp))
        Text(label, style = DfType.title, color = Df.section.accent, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(50))
                .background(Df.colors.raised).pressable(onClick = onSnooze),
            contentAlignment = Alignment.Center,
        ) { Text("Snooze $snooze min", style = DfType.titleSmall, color = Df.colors.textPrimary) }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(50))
                .background(Df.section.accent).pressable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) { Text("Dismiss", style = DfType.titleSmall, color = Color(0xFF03261C)) }
        Spacer(Modifier.height(24.dp))
    }
}

/** Attention Mode screen for a scheduled WhatsApp message. Same look as the alarm screen. */
@Composable
private fun MessageRingScreen(title: String, sub: String, body: String, onOpen: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Df.colors.background).systemBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(title, style = DfType.display, color = Df.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(sub, style = DfType.bodyMedium, color = Df.section.accent, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Df.colors.raised).padding(18.dp),
        ) { Text(body, style = DfType.body, color = Df.colors.textPrimary, maxLines = 8) }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(50))
                .background(Df.section.accent).pressable(onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) { Text("Open WhatsApp", style = DfType.titleSmall, color = Color(0xFF03261C)) }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(50))
                .background(Df.colors.raised).pressable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) { Text("Dismiss", style = DfType.titleSmall, color = Df.colors.textPrimary) }
        Spacer(Modifier.height(24.dp))
    }
}
