package com.dayflow.app.ui.feature.home

import com.dayflow.app.ui.components.doneState
import com.dayflow.app.ui.components.glass
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.TaskItem
import com.dayflow.app.ui.components.OpenChevron
import com.dayflow.app.ui.components.Pill
import com.dayflow.app.ui.components.formatMinute
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.*

fun priorityColor(p: Priority?): Color = when (p) {
    Priority.CRITICAL -> PriorityColors.Critical
    Priority.HIGH -> PriorityColors.High
    Priority.MEDIUM -> PriorityColors.Medium
    null -> PriorityColors.Medium
}

/** PRIORITY: warm amber panel, vertical timeline rail with priority-coloured nodes. */
@Composable
fun ImportantCard(tasks: List<TaskItem>, onToggle: (Long) -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    SectionTheme(DaySection.IMPORTANT) {
        val p = Df.section
        // Open first (by start time), then completed.
        val ordered = tasks.sortedWith(compareBy<TaskItem> { it.done }.thenBy { it.startMinute ?: Int.MAX_VALUE })
        val done = tasks.count { it.done }
        Row(
            modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .glass(tint = p.accent)
        ) {
            // Ember edge: the card's visual "weight".
            Box(Modifier.width(5.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(PriorityColors.Critical, p.accent))))
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Flag, null, tint = p.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("IMPORTANT TASKS", style = DfType.label, color = p.accent, modifier = Modifier.weight(1f))
                    Text("$done / ${tasks.size}", style = DfType.monoLarge, color = Df.colors.textSecondary)
                    OpenChevron(onOpen)
                }
                Spacer(Modifier.height(6.dp))
                Text("Focus today", style = DfType.display, color = Df.colors.textPrimary)
                Spacer(Modifier.height(10.dp))

                if (tasks.isEmpty()) {
                    Text(
                        "Nothing critical today. Add the one thing that deserves serious attention.",
                        style = DfType.body, color = Df.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                ordered.forEachIndexed { i, t ->
                    TimelineRow(t, first = i == 0, last = i == ordered.lastIndex, onToggle = onToggle)
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(t: TaskItem, first: Boolean, last: Boolean, onToggle: (Long) -> Unit) {
    val pc = priorityColor(t.priority)
    val rail = Df.colors.hairline.copy(alpha = 0.22f)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .doneState(t.done)
            .pressable(pressedScale = 0.985f, haptic = HapticFeedbackType.LongPress) { onToggle(t.id) },
    ) {
        Text(
            t.startMinute?.let { formatMinute(it) } ?: "--:--",
            style = DfType.mono,
            color = Df.colors.textTertiary,
            modifier = Modifier.width(44.dp).padding(top = 16.dp),
        )
        Box(
            Modifier
                .width(30.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    drawLine(
                        rail,
                        Offset(x, if (first) 24.dp.toPx() else 0f),
                        Offset(x, if (last) 24.dp.toPx() else size.height),
                        strokeWidth = 2.dp.toPx(),
                    )
                },
        ) {
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .size(20.dp)
                    .align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .background(if (t.done) pc else Df.colors.surface)
                    .border(2.dp, pc, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (t.done) Icon(Icons.Rounded.Check, null, tint = Df.section.onAccent, modifier = Modifier.size(13.dp))
            }
        }
        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(
                t.title,
                style = DfType.titleSmall,
                color = if (t.done) Df.colors.textTertiary else Df.colors.textPrimary,
                textDecoration = if (t.done) TextDecoration.LineThrough else null,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                t.priority?.let { Pill(it.label, pc) }
                if (t.overdue) {
                    Spacer(Modifier.width(8.dp))
                    Pill("Carried", PriorityColors.High)
                }
                t.estimateMin?.let {
                    Spacer(Modifier.width(8.dp))
                    Text("${it} min", style = DfType.mono, color = Df.colors.textTertiary)
                }
            }
        }
    }
}
