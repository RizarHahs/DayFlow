package com.dayflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dayflow.app.domain.time.dayLabel
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfType

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = DfType.label, color = Df.colors.textTertiary, modifier = modifier.padding(bottom = 8.dp))
}

/** Soft filled text field in the current section's tint. */
@Composable
fun DfTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    textStyle: TextStyle = DfType.body,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Df.colors.raised)
            .border(1.dp, Df.colors.hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = textStyle.copy(color = Df.colors.textPrimary),
            cursorBrush = SolidColor(Df.section.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                keyboardType = keyboardType,
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = Df.colors.textTertiary)
                    inner()
                }
            },
        )
    }
}

/** Single-choice pills. */
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    color: ((T) -> Color)? = null,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            val c = color?.invoke(o) ?: Df.section.accent
            Text(
                label(o),
                style = DfType.bodySmall,
                color = if (on) c else Df.colors.textSecondary,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) c.copy(alpha = 0.18f) else Color.Transparent)
                    .border(1.dp, if (on) c else Df.colors.hairline, RoundedCornerShape(50))
                    .pressable { onSelect(o) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

/** Mon..Sun toggles bound to a 7-bit mask (bit0 = Monday). */
@Composable
fun WeekdayPicker(mask: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val names = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        names.forEachIndexed { i, n ->
            val on = (mask shr i) and 1 == 1
            val c = Df.section.accent
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (on) c else Color.Transparent)
                    .border(1.dp, if (on) c else Df.colors.hairline, CircleShape)
                    .pressable { onChange(mask xor (1 shl i)) },
                contentAlignment = Alignment.Center,
            ) { Text(n, style = DfType.bodyMedium, color = if (on) Df.section.onAccent else Df.colors.textSecondary) }
        }
    }
}

/** Tappable value row that opens a time dialog; clearable. */
@Composable
fun TimeField(
    minute: Int?,
    onChange: (Int?) -> Unit,
    placeholder: String = "Not set",
    modifier: Modifier = Modifier,
    defaultMinute: Int = 9 * 60,
    clearable: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    ValueRow(
        text = minute?.let { formatMinute(it) } ?: placeholder,
        set = minute != null,
        onClick = { open = true },
        onClear = if (minute != null && clearable) ({ onChange(null) }) else null,
        modifier = modifier,
        mono = true,
    )
    if (open) {
        TimePickerDialog(
            initial = minute ?: defaultMinute,
            onDismiss = { open = false },
            onConfirm = { onChange(it); open = false },
        )
    }
}

@Composable
fun DayField(day: Int, today: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        ChoiceChips(
            options = listOf(today, today + 1),
            selected = day.takeIf { it == today || it == today + 1 },
            label = { dayLabel(it, today) },
            onSelect = onChange,
        )
        Spacer(Modifier.height(8.dp))
        ValueRow(
            text = if (day == today || day == today + 1) "Pick another date" else dayLabel(day, today),
            set = day != today && day != today + 1,
            onClick = { open = true },
            onClear = null,
        )
    }
    if (open) DatePickerDialogDay(day, onDismiss = { open = false }, onConfirm = { onChange(it); open = false })
}

@Composable
fun OptionalDayField(day: Int?, today: Int, onChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    ValueRow(
        text = day?.let { dayLabel(it, today) } ?: "No deadline",
        set = day != null,
        onClick = { open = true },
        onClear = if (day != null) ({ onChange(null) }) else null,
        modifier = modifier,
    )
    if (open) DatePickerDialogDay(day ?: today, onDismiss = { open = false }, onConfirm = { onChange(it); open = false })
}

@Composable
fun ValueRow(
    text: String,
    set: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Df.colors.raised)
            .border(1.dp, if (set) Df.section.accent.copy(alpha = 0.5f) else Df.colors.hairline, RoundedCornerShape(14.dp))
            .pressable(pressedScale = 0.99f, onClick = onClick)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = if (mono) DfType.monoLarge else DfType.body,
            color = if (set) Df.colors.textPrimary else Df.colors.textTertiary,
            modifier = Modifier.weight(1f),
        )
        if (onClear != null) {
            Box(Modifier.size(44.dp).pressable(haptic = null, onClick = onClear), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, "Clear", tint = Df.colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Digits-only numeric field with a unit suffix (minutes). */
@Composable
fun NumberField(value: Int?, onChange: (Int?) -> Unit, placeholder: String, suffix: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DfTextField(
            value = value?.toString() ?: "",
            onValueChange = { raw -> onChange(raw.filter { it.isDigit() }.take(4).toIntOrNull()) },
            placeholder = placeholder,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(suffix, style = DfType.body, color = Df.colors.textSecondary)
    }
}

/** Full-screen editor chrome: back, title, Save. Bottom bar and FAB are hidden on these routes. */
@Composable
fun EditorScaffold(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).pressable(haptic = null, onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Df.colors.textPrimary)
            }
            Text(title, style = DfType.title, color = Df.colors.textPrimary, modifier = Modifier.weight(1f).padding(start = 4.dp))
            Box(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (saveEnabled) Df.section.accent else Df.section.accent.copy(alpha = 0.3f))
                    .pressable(enabled = saveEnabled, onClick = onSave)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save", style = DfType.titleSmall, color = Df.section.onAccent) }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            content = content,
        )
    }
}

/** Small "open" affordance used on Home card headers. */
@Composable
fun OpenChevron(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(40.dp).pressable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.ChevronRight, "Open", tint = Df.colors.textSecondary, modifier = Modifier.size(22.dp))
    }
}
