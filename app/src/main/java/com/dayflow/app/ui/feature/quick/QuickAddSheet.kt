package com.dayflow.app.ui.feature.quick

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayflow.app.domain.model.NewItem
import com.dayflow.app.domain.model.Period
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.feature.home.priorityColor
import com.dayflow.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * One-sheet capture for every kind. Keyboard is already up; one field + Save.
 * Idea = notebook styling. Important = priority chips. Habit = part-of-day chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(kind: QuickKind, onDismiss: () -> Unit, onSubmit: (NewItem) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val isIdea = kind == QuickKind.IDEA
    val container = if (isIdea) Paper.Sheet else Df.colors.surface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = DfShapes.sheet,
        containerColor = container,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = if (isIdea) Paper.InkSoft.copy(alpha = 0.4f) else Df.colors.textTertiary)
        },
    ) {
        SectionTheme(kind.section) {
            val p = Df.section
            var text by rememberSaveable { mutableStateOf("") }
            var priority by rememberSaveable { mutableStateOf(Priority.HIGH) }
            var period by rememberSaveable { mutableStateOf(Period.MORNING) }
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }

            val submit = {
                val t = text.trim()
                if (t.isNotEmpty()) {
                    onSubmit(NewItem(kind.item, t, priority, period))
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }
            }
            val ink = if (isIdea) Paper.Ink else Df.colors.textPrimary
            val muted = if (isIdea) Paper.InkSoft else Df.colors.textTertiary
            val bodyStyle = if (isIdea) DfType.serifBody.copy(fontSize = 20.sp) else DfType.titleSmall

            Column(
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(kind.icon, null, tint = if (isIdea) Paper.Ink else p.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(kind.label.uppercase(), style = DfType.label, color = if (isIdea) Paper.InkSoft else p.accent)
                }
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = !isIdea,
                    minLines = if (isIdea) 3 else 1,
                    textStyle = bodyStyle.copy(color = ink),
                    cursorBrush = SolidColor(if (isIdea) Paper.Ink else p.accent),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = if (isIdea) ImeAction.Default else ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    decorationBox = { inner ->
                        Box {
                            if (text.isEmpty()) Text(kind.hint, style = bodyStyle, color = muted)
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))

                when (kind) {
                    QuickKind.IMPORTANT -> ChipRow(
                        options = Priority.entries.toList(),
                        selected = priority,
                        label = { it.label },
                        color = { priorityColor(it) },
                        onSelect = { priority = it },
                    )
                    QuickKind.HABIT -> ChipRow(
                        options = Period.entries.toList(),
                        selected = period,
                        label = { it.label },
                        color = { p.accent },
                        onSelect = { period = it },
                    )
                    else -> Unit
                }
                if (kind == QuickKind.IMPORTANT || kind == QuickKind.HABIT) Spacer(Modifier.height(16.dp))

                val canSave = text.isNotBlank()
                val btnBg = if (isIdea) Paper.Ink else p.accent
                val btnFg = if (isIdea) Paper.Sheet else p.onAccent
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (canSave) btnBg else btnBg.copy(alpha = 0.3f))
                        .pressable(enabled = canSave, onClick = submit),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Save", style = DfType.titleSmall, color = btnFg)
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    color: (T) -> Color,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            val c = color(o)
            Text(
                label(o),
                style = DfType.bodySmall,
                color = if (on) c else Df.colors.textSecondary,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) c.copy(alpha = 0.18f) else Color.Transparent)
                    .border(1.dp, if (on) c else Df.colors.hairline, RoundedCornerShape(50))
                    .pressable(onClick = { onSelect(o) })
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}
