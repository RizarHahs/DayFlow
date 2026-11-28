package com.dayflow.app.ui.feature.tasks

import com.dayflow.app.ui.theme.DfShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.TaskKind
import com.dayflow.app.domain.model.TaskItem
import com.dayflow.app.domain.time.dayLabel
import com.dayflow.app.domain.time.maskLabel
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.feature.home.priorityColor
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfType
import com.dayflow.app.ui.theme.PriorityColors

private val rowShape = DfShapes.tile

@Composable
private fun RowCard(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .glass(rowShape)
            .padding(start = 4.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun Grip(handle: Modifier) {
    Box(handle.size(40.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.DragHandle, "Hold and drag to reorder", tint = Df.colors.textTertiary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TaskRow(
    task: TaskItem,
    today: Int,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    menu: List<MenuAction>,
    modifier: Modifier = Modifier,
    handle: Modifier? = null,
    showDate: Boolean = false,
) {
    RowCard(modifier) {
        Box(
            Modifier.size(52.dp).doneState(task.done).pressable(haptic = HapticFeedbackType.LongPress, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) { CheckNode(task.done, size = 26.dp, shape = if (task.kind == TaskKind.NORMAL) RoundedCornerShape(8.dp) else CircleShape) }

        Column(
            Modifier.weight(1f).pressable(pressedScale = 0.99f, haptic = null, onClick = onOpen).padding(vertical = 10.dp),
        ) {
            Text(
                task.title,
                style = DfType.bodyMedium,
                color = if (task.done) Df.colors.textTertiary else Df.colors.textPrimary,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            val meta = buildList {
                if (task.startMinute != null) {
                    add(formatMinute(task.startMinute) + (task.endMinute?.let { "–" + formatMinute(it) } ?: ""))
                }
                task.estimateMin?.let { add("${it}m") }
                if (task.subtasks.isNotEmpty()) add("${task.subtasks.count { it.done }}/${task.subtasks.size} steps")
                if (showDate) add(dayLabel(task.plannedDay, today))
                else task.dueDay?.let { add("due " + dayLabel(it, today)) }
            }
            val pri = task.priority
            if (meta.isNotEmpty() || pri != null || task.overdue) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pri != null) { Pill(pri.label, priorityColor(pri)); Spacer(Modifier.width(8.dp)) }
                    if (task.overdue) { Pill("Carried over", PriorityColors.High); Spacer(Modifier.width(8.dp)) }
                    Text(meta.joinToString("  ·  "), style = DfType.mono, color = Df.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (handle != null) Grip(handle)
        RowMenu(menu)
    }
}

@Composable
fun HabitRow(
    habit: Habit,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    menu: List<MenuAction>,
    handle: Modifier,
    modifier: Modifier = Modifier,
) {
    RowCard(modifier) {
        Box(
            Modifier.size(52.dp).doneState(habit.done).pressable(enabled = habit.dueToday, haptic = HapticFeedbackType.LongPress, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            CheckNode(habit.done, size = 26.dp, color = if (habit.dueToday) Df.section.accent else Df.colors.textTertiary)
        }
        Column(Modifier.weight(1f).pressable(pressedScale = 0.99f, haptic = null, onClick = onOpen).padding(vertical = 10.dp)) {
            Text(
                habit.title, style = DfType.bodyMedium,
                color = if (habit.dueToday) Df.colors.textPrimary else Df.colors.textTertiary,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            val sub = buildList {
                add(maskLabel(habit.repeatMask))
                habit.minute?.let { add(formatMinute(it)) }
                if (habit.streak > 1) add("${habit.streak}-day streak")
                if (!habit.dueToday) add("not today")
            }
            Spacer(Modifier.height(4.dp))
            Text(sub.joinToString("  ·  "), style = DfType.mono, color = Df.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Grip(handle)
        RowMenu(menu)
    }
}
