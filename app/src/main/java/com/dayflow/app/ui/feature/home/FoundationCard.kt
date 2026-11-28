package com.dayflow.app.ui.feature.home

import com.dayflow.app.ui.components.doneState
import com.dayflow.app.ui.components.glass
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.Period
import com.dayflow.app.ui.components.CheckNode
import com.dayflow.app.ui.components.OpenChevron
import com.dayflow.app.ui.components.ProgressRing
import com.dayflow.app.ui.components.formatMinute
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.DaySection
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfType
import com.dayflow.app.ui.theme.SectionTheme

/** DISCIPLINE: calm emerald, big ring, circular check-nodes grouped by part of day. */
@Composable
fun FoundationCard(habits: List<Habit>, onToggle: (Long) -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    SectionTheme(DaySection.FOUNDATION) {
        val p = Df.section
        val done = habits.count { it.done }
        val total = habits.size
        Column(
            modifier
                .fillMaxWidth()
                .glass(tint = p.accent)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DAILY FOUNDATION", style = DfType.label, color = p.accent)
                        OpenChevron(onOpen)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$done", style = DfType.numeral, color = Df.colors.textPrimary)
                        Text(
                            " / $total",
                            style = DfType.title,
                            color = Df.colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Text(
                        if (total > 0 && done == total) "COMPLETED · FOUNDATION LAID" else "COMPLETED",
                        style = DfType.label,
                        color = Df.colors.textTertiary,
                    )
                }
                ProgressRing(
                    progress = if (total == 0) 0f else done.toFloat() / total,
                    modifier = Modifier.size(96.dp),
                    strokeWidth = 10.dp,
                    colors = listOf(p.accent.copy(alpha = 0.55f), p.accent),
                ) {
                    Icon(
                        if (total > 0 && done == total) Icons.Rounded.Check else Icons.Rounded.LocalFireDepartment,
                        null,
                        tint = p.accent,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            if (habits.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "No habits yet. Tap + and add the first one you never want to skip.",
                    style = DfType.body, color = Df.colors.textSecondary,
                )
            }

            Period.entries.forEach { period ->
                val items = habits.filter { it.period == period }.sortedBy { it.minute ?: Int.MAX_VALUE }
                if (items.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(period.label.uppercase(), style = DfType.label, color = Df.colors.textTertiary)
                    Spacer(Modifier.height(4.dp))
                    items.forEach { HabitRow(it, onToggle) }
                }
            }
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, onToggle: (Long) -> Unit) {
    val p = Df.section
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .doneState(habit.done)
            .pressable(pressedScale = 0.98f, haptic = HapticFeedbackType.LongPress) { onToggle(habit.id) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckNode(habit.done, size = 28.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            habit.title,
            style = DfType.bodyMedium,
            color = if (habit.done) Df.colors.textSecondary else Df.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (habit.streak > 1) {
            Icon(Icons.Rounded.LocalFireDepartment, null, tint = p.accent.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            Text("${habit.streak}", style = DfType.mono, color = p.accent.copy(alpha = 0.8f))
            Spacer(Modifier.width(12.dp))
        }
        habit.minute?.let { Text(formatMinute(it), style = DfType.mono, color = Df.colors.textTertiary) }
    }
}
