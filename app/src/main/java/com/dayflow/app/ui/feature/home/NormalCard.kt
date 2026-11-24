package com.dayflow.app.ui.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dayflow.app.domain.model.TaskItem
import com.dayflow.app.ui.components.CheckNode
import com.dayflow.app.ui.components.OpenChevron
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.*

/** PROGRESS: clean cyan list, thin progress line, compact rows, inline add (type, Enter, done). */
@Composable
fun NormalCard(
    targets: List<TaskItem>,
    onToggle: (Long) -> Unit,
    onAdd: (String) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionTheme(DaySection.NORMAL) {
        val p = Df.section
        val shape = RoundedCornerShape(18.dp)
        val done = targets.count { it.done }
        val fraction by animateFloatAsState(
            if (targets.isEmpty()) 0f else done.toFloat() / targets.size, label = "normalBar",
        )
        val remaining = targets.filter { !it.done }
        val finished = targets.filter { it.done }

        Column(
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Df.colors.surface)
                .border(1.dp, Df.colors.hairline, shape)
                .padding(bottom = 14.dp)
        ) {
            Row(
                Modifier.padding(start = 18.dp, end = 6.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("NORMAL TARGETS", style = DfType.label, color = p.accent, modifier = Modifier.weight(1f))
                Text("${remaining.size} left · $done done", style = DfType.mono, color = Df.colors.textSecondary)
                OpenChevron(onOpen)
            }
            Spacer(Modifier.height(12.dp))
            // Thin progress line, edge to edge.
            Box(Modifier.fillMaxWidth().height(3.dp).background(Df.colors.hairline)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(p.accent))
            }
            Spacer(Modifier.height(6.dp))

            Column(Modifier.padding(horizontal = 18.dp)) {
                if (targets.isEmpty()) {
                    Text(
                        "No targets yet. Type one below and press Enter.",
                        style = DfType.body, color = Df.colors.textSecondary,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
                remaining.forEach { TargetRow(it, onToggle) }
                if (finished.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("COMPLETED", style = DfType.label, color = Df.colors.textTertiary)
                    finished.forEach { TargetRow(it, onToggle) }
                }
                Spacer(Modifier.height(10.dp))
                InlineAdd(onAdd)
            }
        }
    }
}

@Composable
private fun TargetRow(t: TaskItem, onToggle: (Long) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .pressable(pressedScale = 0.985f, haptic = HapticFeedbackType.LongPress) { onToggle(t.id) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckNode(t.done, size = 24.dp, shape = RoundedCornerShape(8.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            t.title,
            style = DfType.body,
            color = if (t.done) Df.colors.textTertiary else Df.colors.textPrimary,
            textDecoration = if (t.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        t.estimateMin?.let { Text("${it}m", style = DfType.mono, color = Df.colors.textTertiary) }
    }
}

@Composable
internal fun InlineAdd(onAdd: (String) -> Unit) {
    val p = Df.section
    var text by rememberSaveable { mutableStateOf("") }
    val submit = {
        val t = text.trim()
        if (t.isNotEmpty()) {
            onAdd(t)
            text = ""
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(p.soft)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, null, tint = p.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = DfType.body.copy(color = Df.colors.textPrimary),
            cursorBrush = SolidColor(p.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) Text("Add a target", style = DfType.body, color = Df.colors.textTertiary)
                    inner()
                }
            },
        )
    }
}
