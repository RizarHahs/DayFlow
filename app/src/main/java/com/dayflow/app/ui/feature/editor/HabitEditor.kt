package com.dayflow.app.ui.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.HabitDraft
import com.dayflow.app.domain.model.Period
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.time.maskLabel
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HabitEditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: HabitRepository,
    private val clock: DayClock,
) : ViewModel() {
    val id: Long = handle.get<Long>("id") ?: 0L
    var draft by mutableStateOf(HabitDraft()); private set
    var loaded by mutableStateOf(id == 0L); private set

    init {
        if (id != 0L) viewModelScope.launch {
            repo.getDraft(id)?.let { draft = it }
            loaded = true
        }
    }

    fun edit(block: (HabitDraft) -> HabitDraft) { draft = block(draft) }
    suspend fun save() { repo.save(draft) }
    suspend fun archive() { repo.archive(id, clock.today()) }
}

@Composable
fun HabitEditor(onClose: () -> Unit, modifier: Modifier = Modifier, vm: HabitEditorViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val d = vm.draft
    SectionTheme(DaySection.FOUNDATION) {
        if (!vm.loaded) return@SectionTheme
        EditorScaffold(
            title = if (vm.id == 0L) "New habit" else "Edit habit",
            onBack = onClose,
            onSave = { scope.launch { vm.save(); onClose() } },
            saveEnabled = d.title.isNotBlank() && d.repeatMask != 0,
            modifier = modifier,
        ) {
            FieldLabel("Habit")
            DfTextField(d.title, { v -> vm.edit { it.copy(title = v) } }, "What must you do every day?")
            Spacer(Modifier.height(24.dp))

            FieldLabel("Part of the day")
            ChoiceChips(Period.entries.toList(), d.period, { it.label }, { p -> vm.edit { it.copy(period = p) } })
            Spacer(Modifier.height(24.dp))

            FieldLabel("Preferred time")
            TimeField(d.minute, { m -> vm.edit { it.copy(minute = m) } }, defaultMinute = 7 * 60)
            Spacer(Modifier.height(24.dp))

            FieldLabel("Repeats · ${maskLabel(d.repeatMask)}")
            WeekdayPicker(d.repeatMask, { m -> vm.edit { it.copy(repeatMask = m) } })
            Spacer(Modifier.height(24.dp))

            FieldLabel("Reminder time")
            TimeField(d.reminderMinute, { m -> vm.edit { it.copy(reminderMinute = m) } }, placeholder = "No reminder", defaultMinute = d.minute ?: 8 * 60)
            Text(
                "The time is saved with the habit. Delivery as a real Android alarm/notification is switched on in Phase 4.",
                style = DfType.bodySmall, color = Df.colors.textTertiary, modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(24.dp))

            FieldLabel("Habit timer (optional)")
            NumberField(d.timerSeconds?.div(60), { m -> vm.edit { it.copy(timerSeconds = m?.times(60)) } }, "e.g. 20", "minutes")

            if (vm.id != 0L) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Remove from my routine",
                    style = DfType.bodyMedium, color = PriorityColors.Critical,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp))
                        .pressable { scope.launch { vm.archive(); onClose() } }.padding(vertical = 14.dp),
                )
                Text("Past completions and streak history are kept.", style = DfType.bodySmall, color = Df.colors.textTertiary)
            }
        }
    }
}
