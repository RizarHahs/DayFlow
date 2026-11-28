package com.dayflow.app.ui.feature.settings

import com.dayflow.app.ui.components.glass
import com.dayflow.app.ui.components.GlassBackButton
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.platform.setup.SetupIntents
import com.dayflow.app.platform.setup.SetupStatus
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.dayflow.app.domain.model.ThemeMode
import com.dayflow.app.ui.components.ChoiceChips
import com.dayflow.app.ui.components.TimeField
import java.time.LocalDate
import com.dayflow.app.ui.components.FieldLabel
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier, vm: SettingsViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val status by vm.status.collectAsStateWithLifecycle()
    val notif by vm.notifications.collectAsStateWithLifecycle()
    val look by vm.appearance.collectAsStateWithLifecycle()
    val dataMessage by vm.dataMessage.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    val stamp = LocalDate.now().toString()
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::backupTo) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) pendingRestore = uri }
    val habitsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(vm::exportHabits) }
    val tasksLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(vm::exportTasks) }
    val ideasLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri -> uri?.let(vm::exportIdeas) }
    var notifDenied by rememberSaveable { mutableStateOf(false) }
    var testMessage by rememberSaveable { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifDenied = !granted
        vm.refresh()
    }

    SectionTheme(DaySection.FOUNDATION) {
        Column(modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(onBack)
                Text("Settings", style = DfType.title, color = Df.colors.textPrimary, modifier = Modifier.padding(start = 4.dp))
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                    .navigationBarsPadding().padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Alarm reliability", style = DfType.display, color = Df.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                Summary(status)

                // 1. Notifications
                CheckRow(
                    ok = status.notifications,
                    title = "Notifications",
                    body = "Needed for the ringing alarm screen, timers and reminders.",
                    action = if (status.needsRuntimeNotificationPermission && !notifDenied) "Allow" else "Open settings",
                ) {
                    if (status.needsRuntimeNotificationPermission && !notifDenied && Build.VERSION.SDK_INT >= 33) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else SetupIntents.launch(ctx, SetupIntents.appNotificationSettings(ctx))
                }
                // 2. Exact alarms
                CheckRow(
                    ok = status.exactAlarms,
                    title = "Exact alarms",
                    body = if (status.sdk >= 31) "Lets Android ring alarms at the exact minute, even in Doze." else "Always available on this Android version.",
                    action = "Allow",
                ) { SetupIntents.launch(ctx, SetupIntents.exactAlarm(ctx)) }
                // 3. Alarm channel
                CheckRow(
                    ok = status.alarmChannelOk,
                    title = "Alarm notifications not muted",
                    body = "The Alarms channel must not be switched off in system settings.",
                    action = "Fix",
                ) { SetupIntents.launch(ctx, SetupIntents.alarmChannelSettings(ctx)) }
                // 4. Full screen (Android 14+)
                if (status.sdk >= 34) {
                    CheckRow(
                        ok = status.fullScreen,
                        title = "Full-screen alarm",
                        body = "Shows the alarm over the lock screen.",
                        action = "Allow",
                    ) { SetupIntents.launch(ctx, SetupIntents.fullScreen(ctx)) }
                }
                // 5. Battery
                CheckRow(
                    ok = status.batteryUnrestricted,
                    title = "Unrestricted battery use",
                    body = "Stops the system from stopping DAYFLOW in the background. Recommended on OxygenOS.",
                    action = "Allow",
                ) { SetupIntents.launch(ctx, SetupIntents.batteryRequest(ctx), SetupIntents.batteryList()) }

                OemCard(status) {
                    SetupIntents.launch(ctx, SetupIntents.onePlusAutoLaunch(), SetupIntents.appInfo(ctx))
                }

                // Test
                Text("Test it for real", style = DfType.title, color = Df.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
                Text(
                    "Settings can't prove an alarm will ring on your phone. A test alarm can: lock the phone and wait.",
                    style = DfType.bodySmall, color = Df.colors.textSecondary, modifier = Modifier.padding(start = 4.dp),
                )
                ActionButton("Ring a test alarm in 1 minute") {
                    val ok = vm.testAlarm()
                    testMessage = if (ok) "Test alarm set for " + LocalTime.now().plusMinutes(1).format(DateTimeFormatter.ofPattern("HH:mm")) + ". Lock your phone and wait."
                    else "Android refused an exact alarm. Allow \"Exact alarms\" above first."
                }
                ActionButton("Test a 10-second timer") {
                    vm.testTimer()
                    testMessage = "Timer started. Close the app and watch for the notification."
                }
                testMessage?.let { Text(it, style = DfType.bodyMedium, color = Df.section.accent, modifier = Modifier.padding(horizontal = 4.dp)) }

                // Notification preferences
                Text("Reminders", style = DfType.title, color = Df.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
                ToggleRow("Habit reminders", "At the reminder time you set on each habit", notif.habits, vm::setHabits)
                ToggleRow("Task reminders", "For tasks with a reminder time", notif.tasks, vm::setTasks)
                ToggleRow("Upcoming activities", "Before roadmap activities begin", notif.activities, vm::setActivities)
                if (notif.activities) {
                    FieldLabel("Heads-up before an activity", Modifier.padding(start = 4.dp, top = 4.dp))
                    ChoiceChips(listOf(0, 5, 10, 15), notif.activityLeadMin, { if (it == 0) "At start" else "$it min" }, vm::setLead)
                }
                Text(
                    "Each reminder is shown once. Edit a habit, task or activity to set its time.",
                    style = DfType.bodySmall, color = Df.colors.textTertiary, modifier = Modifier.padding(start = 4.dp),
                )


                // ---- Appearance & formats ----
                SectionTitle("Appearance")
                FieldLabel("Theme", Modifier.padding(start = 4.dp))
                ChoiceChips(ThemeMode.entries.toList(), look.theme, { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, vm::setTheme)
                FieldLabel("Week starts on", Modifier.padding(start = 4.dp, top = 8.dp))
                ChoiceChips(listOf(1, 7, 6), look.weekStart, { when (it) { 1 -> "Monday"; 7 -> "Sunday"; else -> "Saturday" } }, vm::setWeekStart)
                FieldLabel("Time format", Modifier.padding(start = 4.dp, top = 8.dp))
                ChoiceChips(listOf(true, false), look.use24h, { if (it) "24-hour" else "12-hour" }, vm::setUse24h)

                // ---- Timers ----
                SectionTitle("Timers")
                FieldLabel("Default timer length", Modifier.padding(start = 4.dp))
                ChoiceChips(listOf(15, 20, 25, 30, 45, 60), look.defaultTimerMin.takeIf { it in listOf(15, 20, 25, 30, 45, 60) }, { "$it min" }, vm::setDefaultTimer)

                // ---- Daily review ----
                SectionTitle("Daily review")
                ToggleRow("Evening reminder", "A nudge to look back at your day", notif.review, vm::setReview)
                if (notif.review) {
                    FieldLabel("Remind me at", Modifier.padding(start = 4.dp))
                    TimeField(notif.reviewMinute, { m -> if (m != null) vm.setReviewMinute(m) }, defaultMinute = notif.reviewMinute, clearable = false)
                }

                // ---- Your data ----
                SectionTitle("Your data")
                Text(
                    "Everything stays on this phone. A backup is one file you keep wherever you like.",
                    style = DfType.bodySmall, color = Df.colors.textSecondary, modifier = Modifier.padding(start = 4.dp),
                )
                ActionButton(if (busy) "Working..." else "Back up everything") { if (!busy) backupLauncher.launch("dayflow-backup-$stamp.json") }
                ActionButton("Restore from a backup") { if (!busy) restoreLauncher.launch(arrayOf("*/*")) }
                FieldLabel("Export for other apps", Modifier.padding(start = 4.dp, top = 8.dp))
                ActionButton("Habit history (CSV)") { if (!busy) habitsLauncher.launch("dayflow-habits-$stamp.csv") }
                ActionButton("Tasks (CSV)") { if (!busy) tasksLauncher.launch("dayflow-tasks-$stamp.csv") }
                ActionButton("Ideas (Markdown)") { if (!busy) ideasLauncher.launch("dayflow-ideas-$stamp.md") }
                dataMessage?.let { Text(it, style = DfType.bodyMedium, color = Df.section.accent, modifier = Modifier.padding(horizontal = 4.dp)) }

                Text(
                    "${status.manufacturer} ${status.model} · Android ${status.androidRelease} (API ${status.sdk})",
                    style = DfType.mono, color = Df.colors.textTertiary, modifier = Modifier.padding(start = 4.dp, top = 16.dp),
                )
            }
        }
        pendingRestore?.let { uri ->
            AlertDialog(
                onDismissRequest = { pendingRestore = null },
                containerColor = Df.colors.surface,
                title = { Text("Replace everything?", style = DfType.title, color = Df.colors.textPrimary) },
                text = { Text("Restoring replaces all current habits, tasks, ideas, roadmap, alarms and history with the backup. Back up first if you might want today's data. A damaged file changes nothing.", style = DfType.body, color = Df.colors.textSecondary) },
                confirmButton = { TextButton(onClick = { vm.restoreFrom(uri); pendingRestore = null }) { Text("Restore") } },
                dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun Summary(s: SetupStatus) {
    val good = sectionPalette(DaySection.FOUNDATION, Df.colors.isDark).accent
    val c = if (s.alarmsReliable) good else PriorityColors.High
    val shape = DfShapes.tile
    Column(Modifier.fillMaxWidth().clip(shape).background(c.copy(alpha = 0.12f)).border(1.dp, c.copy(alpha = 0.4f), shape).padding(16.dp)) {
        Text(
            if (s.alarmsReliable) "Core permissions are in place" else "Alarms are not guaranteed yet",
            style = DfType.titleSmall, color = Df.colors.textPrimary,
        )
        Text(
            if (s.alarmsReliable) "Battery and vendor settings below can't be fully verified. Use the test."
            else "Fix the items marked below. Until then DAYFLOW will not claim an alarm will ring.",
            style = DfType.bodySmall, color = Df.colors.textSecondary,
        )
    }
}

@Composable
private fun CheckRow(ok: Boolean, title: String, body: String, action: String, onAction: () -> Unit) {
    val good = sectionPalette(DaySection.FOUNDATION, Df.colors.isDark).accent
    val c = if (ok) good else PriorityColors.High
    val shape = DfShapes.tile
    Row(
        Modifier.fillMaxWidth().glass(shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(c.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(if (ok) Icons.Rounded.Check else Icons.Rounded.PriorityHigh, if (ok) "OK" else "Needs attention", tint = c, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = DfType.bodyMedium, color = Df.colors.textPrimary)
            Text(body, style = DfType.bodySmall, color = Df.colors.textSecondary)
        }
        if (!ok) {
            Spacer(Modifier.width(8.dp))
            Text(
                action, style = DfType.bodySmall, color = Df.section.onAccent,
                modifier = Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(50)).background(c)
                    .pressable(onClick = onAction).padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun OemCard(s: SetupStatus, onOpenAutoLaunch: () -> Unit) {
    val shape = DfShapes.tile
    Column(Modifier.fillMaxWidth().glass(shape).padding(16.dp)) {
        Text(if (s.isOnePlus) "OnePlus / OxygenOS extra steps" else "Manufacturer battery settings", style = DfType.titleSmall, color = Df.colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Android can't tell us these are set, so they stay unchecked. Do them once:",
            style = DfType.bodySmall, color = Df.colors.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        listOf(
            "Settings → Apps → DAYFLOW → Battery usage → Allow background activity",
            "Settings → Apps → DAYFLOW → turn on Auto-launch (if you see it)",
            "Open Recents, tap the DAYFLOW card's menu, then Lock so cleaning can't close it",
            "Settings → Battery → Battery optimization → DAYFLOW → Don't optimize",
        ).forEachIndexed { i, t ->
            Text("${i + 1}. $t", style = DfType.bodySmall, color = Df.colors.textPrimary, modifier = Modifier.padding(vertical = 3.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Open Auto-launch / App info", style = DfType.bodyMedium, color = Df.section.accent,
            modifier = Modifier.heightIn(min = 44.dp).pressable(onClick = onOpenAutoLaunch).padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 54.dp)
            .pressable(onClick = onClick)
            .glass(RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = DfType.bodyMedium, color = Df.colors.textPrimary) }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DfType.bodyMedium, color = Df.colors.textPrimary)
            Text(body, style = DfType.bodySmall, color = Df.colors.textTertiary)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Df.section.onAccent, checkedTrackColor = Df.section.accent,
                uncheckedThumbColor = Df.colors.textSecondary, uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Df.colors.textTertiary,
            ),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = DfType.title, color = Df.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, top = 16.dp))
}
