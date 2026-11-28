package com.dayflow.app.ui.feature.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.domain.model.TimerSession
import com.dayflow.app.domain.model.TimerState
import com.dayflow.app.domain.model.TimerTarget
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.theme.*

private val presets = listOf(1, 5, 10, 15, 25)

@Composable
fun TimerScreen(vm: TimerViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val session by vm.current.collectAsStateWithLifecycle()
    SectionTheme(DaySection.NORMAL) {
        Column(modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(onBack)
                Text("Timer", style = DfType.title, color = Df.colors.textPrimary, modifier = Modifier.padding(start = 4.dp))
            }
            val s = session
            if (s == null) SetupPanel(vm) else RunningPanel(s, vm)
        }
    }
}

@Composable
private fun SetupPanel(vm: TimerViewModel) {
    val defaultMin by vm.defaultMinutes.collectAsStateWithLifecycle()
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    val minutes = picked ?: defaultMin
    var label by rememberSaveable { mutableStateOf("") }
    var targetType by rememberSaveable { mutableStateOf<TimerTarget?>(null) }
    var targetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val habits by vm.habitOptions.collectAsStateWithLifecycle()
    val tasks by vm.taskOptions.collectAsStateWithLifecycle()
    val activities by vm.activityOptions.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
        Text("Start a timer", style = DfType.display, color = Df.colors.textPrimary)
        Spacer(Modifier.height(20.dp))

        FieldLabel("Quick start")
        ChoiceChips(presets, minutes.takeIf { it in presets }, { "$it min" }, { picked = it })
        Spacer(Modifier.height(20.dp))

        FieldLabel("Custom length")
        NumberField(minutes, { picked = it ?: 0 }, "Minutes", "minutes")
        Spacer(Modifier.height(20.dp))

        FieldLabel("Label (optional)")
        DfTextField(label, { label = it }, "e.g. Deep work")
        Spacer(Modifier.height(20.dp))

        FieldLabel("Attach to")
        ChoiceChips(
            options = listOf<TimerTarget?>(null, TimerTarget.HABIT, TimerTarget.TASK, TimerTarget.ACTIVITY),
            selected = targetType,
            label = { when (it) { null -> "Nothing"; TimerTarget.HABIT -> "Habit"; TimerTarget.TASK -> "Task"; TimerTarget.ACTIVITY -> "Activity" } },
            onSelect = { targetType = it; targetId = null },
        )
        Spacer(Modifier.height(10.dp))
        when (targetType) {
            TimerTarget.HABIT -> ChoiceChips(habits.map { it.id }, targetId, { id -> habits.firstOrNull { it.id == id }?.title ?: "" }, { id ->
                targetId = id; habits.firstOrNull { it.id == id }?.let { h -> if (label.isBlank()) label = h.title; h.timerSeconds?.let { s -> picked = (s / 60).coerceAtLeast(1) } }
            })
            TimerTarget.TASK -> ChoiceChips(tasks.map { it.id }, targetId, { id -> tasks.firstOrNull { it.id == id }?.title ?: "" }, { id ->
                targetId = id; tasks.firstOrNull { it.id == id }?.let { t -> if (label.isBlank()) label = t.title; (t.timerSeconds?.div(60) ?: t.estimateMin)?.let { m -> picked = m.coerceAtLeast(1) } }
            })
            TimerTarget.ACTIVITY -> ChoiceChips(activities.map { it.id }, targetId, { id -> activities.firstOrNull { it.id == id }?.title ?: "" }, { id ->
                targetId = id; activities.firstOrNull { it.id == id }?.let { a -> if (label.isBlank()) label = a.title; picked = (a.endMinute - a.startMinute).coerceAtLeast(1) }
            })
            null -> Unit
        }
        Spacer(Modifier.height(28.dp))

        NotificationHint()
        Spacer(Modifier.height(12.dp))
        val canStart = minutes in 1..999
        Box(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(50))
                .background(if (canStart) Df.section.accent else Df.section.accent.copy(alpha = 0.3f))
                .pressable(enabled = canStart) { vm.start(label, minutes, targetType, targetId) },
            contentAlignment = Alignment.Center,
        ) { Text("Start ${formatDuration(minutes * 60_000L)}", style = DfType.titleSmall, color = Df.section.onAccent) }
        Spacer(Modifier.height(10.dp))
        Text(
            "Keeps counting if you close the app or restart the phone, and notifies you when it ends.",
            style = DfType.bodySmall, color = Df.colors.textTertiary,
        )
    }
}

@Composable
private fun RunningPanel(s: TimerSession, vm: TimerViewModel) {
    val remaining = rememberRemainingMs(s, vm.bootCount)
    val finished = s.state == TimerState.FINISHED
    val paused = s.state == TimerState.PAUSED
    val fraction = if (s.durationMs == 0L) 0f else (remaining.toFloat() / s.durationMs).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        ProgressRing(fraction, Modifier.size(280.dp), strokeWidth = 14.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (finished) "Done" else formatDuration(remaining),
                    style = DfType.monoLarge.copy(fontSize = 56.sp, lineHeight = 62.sp), color = Df.colors.textPrimary,
                )
                Text(
                    when { finished -> "Time's up"; paused -> "PAUSED"; else -> "REMAINING" },
                    style = DfType.label, color = if (paused) PriorityColors.High else Df.colors.textTertiary,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(s.label.ifBlank { "Timer" }, style = DfType.title, color = Df.colors.textPrimary)
        Text("of ${formatDuration(s.durationMs)}", style = DfType.mono, color = Df.colors.textTertiary)
        Spacer(Modifier.weight(1f))

        if (finished) {
            BigButton("Dismiss", primary = true) { vm.dismiss() }
            Spacer(Modifier.height(12.dp))
            BigButton("Run again", primary = false) { vm.reset() }
        } else {
            BigButton(if (paused) "Resume" else "Pause", primary = true) { if (paused) vm.resume() else vm.pause() }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("Reset", primary = false, modifier = Modifier.weight(1f)) { vm.reset() }
                BigButton("Cancel", primary = false, modifier = Modifier.weight(1f)) { vm.cancel() }
            }
        }
    }
}

@Composable
private fun BigButton(text: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(50))
            .background(if (primary) Df.section.accent else Df.colors.raised)
            .then(if (primary) Modifier else Modifier.border(1.dp, Df.colors.hairline, RoundedCornerShape(50)))
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = DfType.titleSmall, color = if (primary) Df.section.onAccent else Df.colors.textPrimary) }
}
