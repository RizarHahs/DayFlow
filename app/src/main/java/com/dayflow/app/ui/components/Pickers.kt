package com.dayflow.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dayflow.app.ui.theme.Df

private const val MS_PER_DAY = 86_400_000L

/** 24-hour dial picker. Returns minute-of-day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = TimeFormat.is24h)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Df.colors.surface,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) } },
    )
}

/** Material date picker. Material returns UTC midnight millis, which maps 1:1 to an epoch day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogDay(initialDay: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDay * MS_PER_DAY)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onConfirm((it / MS_PER_DAY).toInt()) } }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}
