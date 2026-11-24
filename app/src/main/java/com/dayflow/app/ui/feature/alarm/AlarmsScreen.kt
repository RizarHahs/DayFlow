package com.dayflow.app.ui.feature.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.domain.model.Alarm
import com.dayflow.app.domain.model.AlarmStatus
import com.dayflow.app.domain.time.maskLabel
import com.dayflow.app.ui.components.EmptyState
import com.dayflow.app.ui.components.Pill
import com.dayflow.app.ui.components.formatMinute
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.*

private fun untilText(at: Long): String {
    val mins = ((at - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
    val d = mins / 1440; val h = (mins % 1440) / 60; val m = mins % 60
    return when {
        d > 0 -> "${d}d ${h}h"
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m"
    }
}

@Composable
fun AlarmsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AlarmsViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    SectionTheme(DaySection.IMPORTANT) {
        Column(modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).pressable(haptic = null, onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = Df.colors.textPrimary)
                }
                Text("Alarms", style = DfType.title, color = Df.colors.textPrimary, modifier = Modifier.weight(1f).padding(start = 4.dp))
                Row(
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(Df.section.accent)
                        .pressable(onClick = onAdd).padding(horizontal = 18.dp),
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { HealthBanner(s.setup.alarmsReliable, s.setup.attentionCount, onOpenSettings) }
                s.next?.let { n -> item { NextCard(n) } }
                if (s.rows.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                            EmptyState(Icons.Rounded.Alarm, "No alarms yet", "Add a wake-up alarm, or tie one to a habit or roadmap activity.")
                        }
                    }
                }
                items(s.rows, key = { it.alarm.id }) { row ->
                    AlarmCard(row, onClick = { onEdit(row.alarm.id) }, onToggle = { vm.toggle(row.alarm.id, it) })
                }
            }
        }
    }
}

@Composable
private fun HealthBanner(reliable: Boolean, attention: Int, onFix: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val c = if (reliable) sectionPalette(DaySection.FOUNDATION, Df.colors.isDark).accent else PriorityColors.High
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.copy(alpha = 0.12f)).border(1.dp, c.copy(alpha = 0.4f), shape)
            .pressable(pressedScale = 0.99f, haptic = null, onClick = onFix).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (reliable) "Alarm permissions are in place" else "Alarms may not ring reliably",
                style = DfType.titleSmall, color = Df.colors.textPrimary,
            )
            Text(
                if (reliable) "Run the test alarm in Settings to be sure on this phone."
                else "$attention setting${if (attention == 1) "" else "s"} need attention. Tap to fix.",
                style = DfType.bodySmall, color = Df.colors.textSecondary,
            )
        }
        Text("Setup", style = DfType.bodyMedium, color = c)
    }
}

@Composable
private fun NextCard(a: Alarm) {
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Df.colors.surface).border(1.dp, Df.colors.hairline, shape).padding(16.dp)) {
        Text("NEXT ALARM", style = DfType.label, color = Df.section.accent)
        Spacer(Modifier.height(4.dp))
        Text(
            "${a.label.ifBlank { a.kind.label }} rings in ${untilText(a.nextTriggerAt!!)}",
            style = DfType.titleSmall, color = Df.colors.textPrimary,
        )
    }
}

@Composable
private fun AlarmCard(row: AlarmRow, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val a = row.alarm
    val shape = RoundedCornerShape(18.dp)
    val on = a.enabled
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Df.colors.surface).border(1.dp, Df.colors.hairline, shape)
            .pressable(pressedScale = 0.99f, haptic = null, onClick = onClick).padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                formatMinute(a.minute),
                style = DfType.monoLarge.copy(fontSize = 34.sp, lineHeight = 38.sp),
                color = if (on) Df.colors.textPrimary else Df.colors.textTertiary,
            )
            Text(
                a.label.ifBlank { a.kind.label } + "  ·  " + if (a.repeatMask == 0) "Once" else maskLabel(a.repeatMask),
                style = DfType.bodySmall, color = Df.colors.textSecondary, maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            when (row.status) {
                AlarmStatus.ARMED -> Pill("Armed", sectionPalette(DaySection.FOUNDATION, Df.colors.isDark).accent)
                AlarmStatus.NEEDS_PERMISSION -> Pill("Needs permission · not guaranteed", PriorityColors.High)
                AlarmStatus.DISABLED -> Pill("Off", Df.colors.textTertiary)
            }
        }
        Switch(
            checked = on,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Df.section.onAccent, checkedTrackColor = Df.section.accent,
                uncheckedThumbColor = Df.colors.textSecondary, uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Df.colors.textTertiary,
            ),
        )
    }
}
