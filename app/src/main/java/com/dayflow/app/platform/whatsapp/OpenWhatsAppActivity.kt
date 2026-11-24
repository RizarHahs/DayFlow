package com.dayflow.app.platform.whatsapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.lifecycleScope
import com.dayflow.app.domain.model.ScheduledMessage
import com.dayflow.app.domain.repo.ScheduledMessageController
import com.dayflow.app.platform.Actions
import com.dayflow.app.platform.alarm.AlarmRingService
import com.dayflow.app.ui.theme.DayflowTheme
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Where "Open WhatsApp" lands (notification tap, notification button, Attention screen, or the list).
 * It has no screen of its own unless something needs explaining: it picks WhatsApp Business, else offers
 * regular WhatsApp, else explains. It is launched by a direct user tap, so Android allows it to start
 * WhatsApp. It never sends: the message is only prefilled.
 */
@AndroidEntryPoint
class OpenWhatsAppActivity : ComponentActivity() {

    @Inject lateinit var controller: ScheduledMessageController
    @Inject lateinit var launcher: WhatsAppLauncher
    @Inject lateinit var notifier: MessageNotifier

    private sealed interface UiState {
        data object Working : UiState
        data object Gone : UiState
        data class AskRegular(val m: ScheduledMessage) : UiState
        data class NoWhatsApp(val m: ScheduledMessage) : UiState
        data class LaunchFailed(val m: ScheduledMessage, val target: WhatsAppTarget) : UiState
    }

    private var ui by mutableStateOf<UiState>(UiState.Working)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(Actions.EXTRA_MESSAGE_ID, -1L)
        setContent { DayflowTheme(darkTheme = true) { Dialogs() } }
        lifecycleScope.launch { start(id) }
    }

    private suspend fun start(id: Long) {
        val m = if (id >= 0) controller.get(id) else null
        if (m == null) { ui = UiState.Gone; return }
        val a = launcher.availability()
        when {
            a.business -> open(m, WhatsAppTarget.BUSINESS)
            a.regular -> ui = UiState.AskRegular(m)
            else -> {
                controller.markFailed(m.id, "Neither WhatsApp Business nor WhatsApp is installed.")
                ui = UiState.NoWhatsApp(m)
            }
        }
    }

    private suspend fun open(m: ScheduledMessage, target: WhatsAppTarget) {
        if (launcher.launch(this, m.phone, m.message, target)) {
            controller.markOpened(m.id)
            // If Attention Mode is ringing for this message, silence it. A different alarm is left alone.
            startService(AlarmRingService.dismissMessageIntent(this, m.id))
            finish()
        } else {
            controller.markFailed(m.id, "${target.label} could not be opened.")
            ui = UiState.LaunchFailed(m, target)
        }
    }

    @androidx.compose.runtime.Composable
    private fun Dialogs() {
        val clipboard = LocalClipboardManager.current
        fun copy(text: String) { clipboard.setText(AnnotatedString(text)) }
        when (val s = ui) {
            UiState.Working -> Unit
            UiState.Gone -> Info(
                title = "Message not found",
                text = "This scheduled message no longer exists, so there is nothing to open.",
                confirmText = "Close", onConfirm = { finish() },
            )
            is UiState.AskRegular -> Info(
                title = "WhatsApp Business isn't installed",
                text = "DAYFLOW prefers WhatsApp Business, but regular WhatsApp is installed. Open the chat with " +
                    "${s.m.displayName} there instead? Your message will be prefilled and you press Send yourself.",
                confirmText = "Open WhatsApp", onConfirm = { lifecycleScope.launch { open(s.m, WhatsAppTarget.REGULAR) } },
                dismissText = "Not now", onDismiss = { finish() },
            )
            is UiState.NoWhatsApp -> Info(
                title = "WhatsApp isn't installed",
                text = "Neither WhatsApp Business nor WhatsApp was found on this phone. Install one, then open this message " +
                    "again from Scheduled Messages. Your message is kept.\n\n${s.m.message}",
                confirmText = "Copy message", onConfirm = { copy(s.m.message); finish() },
                dismissText = "Close", onDismiss = { finish() },
            )
            is UiState.LaunchFailed -> {
                val canTryRegular = s.target == WhatsAppTarget.BUSINESS && launcher.availability().regular
                if (canTryRegular) {
                    Info(
                        title = "Couldn't open ${s.target.label}",
                        text = "Android didn't let DAYFLOW open the chat. You can try regular WhatsApp, or copy the message and paste it yourself.",
                        confirmText = "Try WhatsApp", onConfirm = { lifecycleScope.launch { open(s.m, WhatsAppTarget.REGULAR) } },
                        dismissText = "Copy message", onDismiss = { copy(s.m.message); finish() },
                    )
                } else {
                    Info(
                        title = "Couldn't open ${s.target.label}",
                        text = "Android didn't let DAYFLOW open the chat. Copy the message and paste it into the chat yourself.",
                        confirmText = "Copy message", onConfirm = { copy(s.m.message); finish() },
                        dismissText = "Close", onDismiss = { finish() },
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Info(
        title: String,
        text: String,
        confirmText: String,
        onConfirm: () -> Unit,
        dismissText: String? = null,
        onDismiss: () -> Unit = {},
    ) {
        AlertDialog(
            onDismissRequest = { finish() },
            containerColor = Df.colors.surface,
            title = { Text(title, style = DfType.title, color = Df.colors.textPrimary) },
            text = { Text(text, style = DfType.body, color = Df.colors.textSecondary) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
            dismissButton = if (dismissText == null) null else ({ TextButton(onClick = onDismiss) { Text(dismissText) } }),
        )
    }

    companion object {
        fun intent(ctx: Context, messageId: Long): Intent =
            Intent(ctx, OpenWhatsAppActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Actions.EXTRA_MESSAGE_ID, messageId)
    }
}
