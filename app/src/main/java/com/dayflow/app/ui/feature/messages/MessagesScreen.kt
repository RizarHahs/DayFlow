package com.dayflow.app.ui.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.domain.model.MessageStatus
import com.dayflow.app.domain.model.ScheduledMessage
import com.dayflow.app.domain.time.dayLabel
import com.dayflow.app.platform.setup.SetupIntents
import com.dayflow.app.platform.whatsapp.OpenWhatsAppActivity
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.theme.*

@Composable
fun statusColor(s: MessageStatus): Color = when (s) {
    MessageStatus.SCHEDULED -> Df.section.accent
    MessageStatus.TRIGGERED -> PriorityColors.High
    MessageStatus.OPENED -> sectionPalette(DaySection.FOUNDATION, Df.colors.isDark).accent
    MessageStatus.FAILED -> PriorityColors.Critical
}

@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    vm: MessagesViewModel = hiltViewModel(),
) {
    val ctx = LocalContext.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf<ScheduledMessage?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    SectionTheme(DaySection.NORMAL) {
        Column(modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).pressable(haptic = null, onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = Df.colors.textPrimary)
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("SCHEDULED MESSAGES", style = DfType.label, color = Df.section.accent)
                    Text("WhatsApp reminders", style = DfType.title, color = Df.colors.textPrimary)
                }
                Row(
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(Df.section.accent)
                        .pressable(onClick = onNew).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Df.section.onAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New", style = DfType.bodyMedium, color = Df.section.onAccent)
                }
                Spacer(Modifier.width(8.dp))
            }
            val s = ui ?: return@Column
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { NotificationHint() }
                if (!s.exactAlarmOk) item {
                    Warning(
                        "Exact alarms are off",
                        "Android may deliver your reminders a few minutes late.",
                        "Allow",
                    ) { SetupIntents.launch(ctx, SetupIntents.exactAlarm(ctx)) }
                }
                item { WhatsAppStatus(s.whatsApp) }
                item { OptionsCard(s.settings.attentionMode, s.settings.defaultCountryCode, vm::setAttentionMode, vm::setDefaultCountryCode) }

                if (s.isEmpty) item {
                    Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                        EmptyState(
                            Icons.Rounded.Chat, "No scheduled messages",
                            "Pick a person, an occasion and a time. DAYFLOW alerts you then and opens the chat with your message ready. You press Send.",
                        )
                    }
                }
                if (s.needsAction.isNotEmpty()) {
                    item { Heading("Ready to send") }
                    items(s.needsAction, key = { it.id }) { MessageCard(it, s.today, { onEdit(it.id) }, { confirmDelete = it }) }
                }
                if (s.upcoming.isNotEmpty()) {
                    item { Heading("Upcoming") }
                    items(s.upcoming, key = { it.id }) { MessageCard(it, s.today, { onEdit(it.id) }, { confirmDelete = it }) }
                }
                if (s.opened.isNotEmpty()) {
                    item { Heading("Opened in WhatsApp") }
                    items(s.opened, key = { it.id }) { MessageCard(it, s.today, { onEdit(it.id) }, { confirmDelete = it }) }
                }
                item {
                    Text(
                        "DAYFLOW never sends messages. It reminds you, opens the chat with your text ready, and you press Send. " +
                            "\"Opened\" means WhatsApp was opened, not that the message was sent.",
                        style = DfType.bodySmall, color = Df.colors.textTertiary, modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
        confirmDelete?.let { m ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                containerColor = Df.colors.surface,
                title = { Text("Delete this message?", style = DfType.title, color = Df.colors.textPrimary) },
                text = { Text("The scheduled reminder for ${m.displayName} will be cancelled and removed.", style = DfType.body, color = Df.colors.textSecondary) },
                confirmButton = { TextButton(onClick = { vm.delete(m.id); confirmDelete = null }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text.uppercase(), style = DfType.label, color = Df.colors.textTertiary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

@Composable
private fun Warning(title: String, body: String, action: String, onAction: () -> Unit) {
    val c = PriorityColors.High
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.14f)).padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(title, style = DfType.bodyMedium, color = Df.colors.textPrimary)
            Text(body, style = DfType.bodySmall, color = Df.colors.textSecondary)
        }
        Text(
            action, style = DfType.bodyMedium, color = c,
            modifier = Modifier.heightIn(min = 44.dp).pressable(onClick = onAction).padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun WhatsAppStatus(a: com.dayflow.app.platform.whatsapp.WhatsAppAvailability) {
    val good = sectionPalette(DaySection.FOUNDATION, Df.colors.isDark).accent
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Df.colors.surface).border(1.dp, Df.colors.hairline, shape).padding(14.dp)) {
        Text("WHATSAPP ON THIS PHONE", style = DfType.label, color = Df.colors.textTertiary)
        Spacer(Modifier.height(6.dp))
        Text(
            "WhatsApp Business: " + if (a.business) "installed (preferred)" else "not found",
            style = DfType.bodySmall, color = if (a.business) good else Df.colors.textSecondary,
        )
        Text(
            "WhatsApp: " + if (a.regular) "installed" else "not found",
            style = DfType.bodySmall, color = if (a.regular) good else Df.colors.textSecondary,
        )
        if (!a.any) {
            Spacer(Modifier.height(6.dp))
            Text("Install WhatsApp Business or WhatsApp to open chats. You can still schedule messages now.", style = DfType.bodySmall, color = PriorityColors.High)
        } else if (!a.business) {
            Spacer(Modifier.height(6.dp))
            Text("DAYFLOW prefers WhatsApp Business. Without it, it will ask before using regular WhatsApp.", style = DfType.bodySmall, color = Df.colors.textTertiary)
        }
    }
}

@Composable
private fun OptionsCard(attention: Boolean, countryCode: String, onAttention: (Boolean) -> Unit, onCountryCode: (String) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    var code by remember(countryCode) { mutableStateOf(countryCode) }
    Column(Modifier.fillMaxWidth().clip(shape).background(Df.colors.surface).border(1.dp, Df.colors.hairline, shape).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Attention Mode", style = DfType.bodyMedium, color = Df.colors.textPrimary)
                Text(
                    "Rings like an alarm (sound, vibration, full-screen on the lock screen) instead of a quiet notification.",
                    style = DfType.bodySmall, color = Df.colors.textTertiary,
                )
            }
            Switch(
                checked = attention, onCheckedChange = onAttention,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Df.section.onAccent, checkedTrackColor = Df.section.accent,
                    uncheckedThumbColor = Df.colors.textSecondary, uncheckedTrackColor = Color.Transparent,
                    uncheckedBorderColor = Df.colors.textTertiary,
                ),
            )
        }
        if (attention) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Android doesn't let any app force WhatsApp to open over another app, so you will always tap \"Open WhatsApp\" yourself.",
                style = DfType.bodySmall, color = Df.colors.textTertiary,
            )
        }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Default country code (optional)")
        DfTextField(
            code, { v -> code = v.filter { it.isDigit() || it == '+' }.take(5); onCountryCode(code) },
            "e.g. +977", keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
        )
        Text("Lets numbers written locally, like 9812345678, be completed automatically.", style = DfType.bodySmall, color = Df.colors.textTertiary, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun MessageCard(m: ScheduledMessage, today: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    val c = statusColor(m.status)
    val shape = RoundedCornerShape(18.dp)
    val openLabel = if (m.status == MessageStatus.SCHEDULED) "Open now" else "Open WhatsApp"
    val open = { ctx.startActivity(OpenWhatsAppActivity.intent(ctx, m.id)) }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Df.colors.surface)
            .border(1.dp, if (m.status == MessageStatus.TRIGGERED || m.status == MessageStatus.FAILED) c.copy(alpha = 0.5f) else Df.colors.hairline, shape)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.displayName, style = DfType.titleSmall, color = Df.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(m.occasion, Df.section.accent)
                    Spacer(Modifier.width(8.dp))
                    Pill(m.status.label, c)
                }
            }
            RowMenu(
                buildList {
                    add(MenuAction(openLabel) { open() })
                    add(MenuAction(if (m.status == MessageStatus.SCHEDULED) "Edit" else "Edit or reschedule") { onEdit() })
                    add(MenuAction("Delete", destructive = true) { onDelete() })
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(dayLabel(m.day, today) + "  ·  " + formatMinute(m.minute), style = DfType.monoLarge, color = Df.colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            m.message, style = DfType.body, color = Df.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 12.dp),
        )
        if (m.status == MessageStatus.FAILED && !m.failureReason.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(m.failureReason, style = DfType.bodySmall, color = c, modifier = Modifier.padding(end = 12.dp))
        }
        if (m.status == MessageStatus.TRIGGERED || m.status == MessageStatus.FAILED) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.padding(end = 12.dp).fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(50))
                    .background(Df.section.accent).pressable(onClick = { open() }),
                contentAlignment = Alignment.Center,
            ) { Text(openLabel, style = DfType.bodyMedium, color = Df.section.onAccent) }
        }
    }
}
