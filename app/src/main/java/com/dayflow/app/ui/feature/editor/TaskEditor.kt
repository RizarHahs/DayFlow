package com.dayflow.app.ui.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.feature.home.priorityColor
import com.dayflow.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: TaskRepository,
    clock: DayClock,
) : ViewModel() {
    val id: Long = handle.get<Long>("id") ?: 0L
    val today: Int = clock.today()
    private val startKind: TaskKind = runCatching { TaskKind.valueOf(handle.get<String>("kind") ?: "") }.getOrDefault(TaskKind.NORMAL)

    var draft by mutableStateOf(
        TaskDraft(
            kind = startKind, plannedDay = today,
            priority = if (startKind == TaskKind.IMPORTANT) Priority.HIGH else null,
            rolloverPolicy = RolloverPolicy.defaultFor(startKind),
        )
    ); private set
    var loaded by mutableStateOf(id == 0L); private set

    init {
        if (id != 0L) viewModelScope.launch {
            repo.getDraft(id)?.let { draft = it }
            loaded = true
        }
    }

    fun edit(block: (TaskDraft) -> TaskDraft) { draft = block(draft) }
    suspend fun save() { repo.save(draft) }
    suspend fun delete() { repo.delete(id) }
}

@Composable
fun TaskEditor(onClose: () -> Unit, modifier: Modifier = Modifier, vm: TaskEditorViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val d = vm.draft
    var confirmDelete by remember { mutableStateOf(false) }
    val section = when (d.kind) {
        TaskKind.IMPORTANT -> DaySection.IMPORTANT
        TaskKind.NORMAL -> DaySection.NORMAL
        TaskKind.EXTRA -> DaySection.EXTRA
    }
    SectionTheme(section) {
        if (!vm.loaded) return@SectionTheme
        val timesOk = d.startMinute == null || d.endMinute == null || d.endMinute > d.startMinute
        val label = when (d.kind) { TaskKind.IMPORTANT -> "important task"; TaskKind.NORMAL -> "target"; TaskKind.EXTRA -> "extra target" }
        EditorScaffold(
            title = if (vm.id == 0L) "New $label" else "Edit $label",
            onBack = onClose,
            onSave = { scope.launch { vm.save(); onClose() } },
            saveEnabled = d.title.isNotBlank() && timesOk,
            modifier = modifier,
        ) {
            FieldLabel("Title")
            DfTextField(d.title, { v -> vm.edit { it.copy(title = v) } }, "What needs doing?")
            Spacer(Modifier.height(20.dp))
            FieldLabel("Notes")
            DfTextField(d.description, { v -> vm.edit { it.copy(description = v) } }, "Optional details", singleLine = false, minLines = 3)

            if (d.kind == TaskKind.IMPORTANT) {
                Spacer(Modifier.height(24.dp))
                FieldLabel("Priority")
                ChoiceChips(Priority.entries.toList(), d.priority, { it.label }, { p -> vm.edit { it.copy(priority = p) } }, color = { priorityColor(it) })
            }

            Spacer(Modifier.height(24.dp))
            FieldLabel("Planned for")
            DayField(d.plannedDay, vm.today, { day -> vm.edit { it.copy(plannedDay = day) } })

            Spacer(Modifier.height(24.dp))
            FieldLabel("Deadline")
            OptionalDayField(d.dueDay, vm.today, { day -> vm.edit { it.copy(dueDay = day) } })

            if (d.kind == TaskKind.IMPORTANT) {
                Spacer(Modifier.height(24.dp))
                FieldLabel("Start")
                TimeField(d.startMinute, { m -> vm.edit { it.copy(startMinute = m) } }, defaultMinute = 9 * 60)
                Spacer(Modifier.height(12.dp))
                FieldLabel("End")
                TimeField(d.endMinute, { m -> vm.edit { it.copy(endMinute = m) } }, defaultMinute = (d.startMinute ?: 9 * 60) + 60)
                if (!timesOk) Text("End must be after start.", style = DfType.bodySmall, color = PriorityColors.Critical, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(24.dp))
            FieldLabel("Estimated time")
            NumberField(d.estimateMin, { m -> vm.edit { it.copy(estimateMin = m) } }, "Optional", "minutes")

            if (d.kind != TaskKind.EXTRA) {
                Spacer(Modifier.height(24.dp))
                FieldLabel("Reminder")
                TimeField(d.reminderMinute, { m -> vm.edit { it.copy(reminderMinute = m) } }, placeholder = "No reminder", defaultMinute = d.startMinute ?: 9 * 60)
                Text("Saved with the task. Real notifications arrive in Phase 4.", style = DfType.bodySmall, color = Df.colors.textTertiary, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(24.dp))
            FieldLabel("Attached timer")
            NumberField(d.timerSeconds?.div(60), { m -> vm.edit { it.copy(timerSeconds = m?.times(60)) } }, "Optional", "minutes")

            if (d.kind != TaskKind.EXTRA) {
                Spacer(Modifier.height(24.dp))
                FieldLabel("Steps")
                d.subtasks.forEachIndexed { i, st ->
                    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        DfTextField(
                            st.title,
                            { v -> vm.edit { it.copy(subtasks = it.subtasks.toMutableList().also { l -> l[i] = st.copy(title = v) }) } },
                            "Step ${i + 1}", modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier.size(48.dp).pressable(haptic = null) { vm.edit { it.copy(subtasks = it.subtasks.filterIndexed { j, _ -> j != i }) } },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Close, "Remove step", tint = Df.colors.textSecondary, modifier = Modifier.size(18.dp)) }
                    }
                }
                Row(
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50))
                        .pressable { vm.edit { it.copy(subtasks = it.subtasks + SubtaskDraft(title = "")) } }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Df.section.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add a step", style = DfType.bodyMedium, color = Df.section.accent)
                }
            }

            Spacer(Modifier.height(24.dp))
            FieldLabel("If it isn't finished by tomorrow")
            ChoiceChips(RolloverPolicy.entries.toList(), d.rolloverPolicy, { it.label }, { p -> vm.edit { it.copy(rolloverPolicy = p) } })

            if (vm.id != 0L) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Delete permanently", style = DfType.bodyMedium, color = PriorityColors.Critical,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp))
                        .pressable { confirmDelete = true }.padding(vertical = 14.dp),
                )
            }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                containerColor = Df.colors.surface,
                title = { Text("Delete this task?", style = DfType.title, color = Df.colors.textPrimary) },
                text = { Text("It will be removed for good. To keep a record, use Drop it from the list menu instead.", style = DfType.body, color = Df.colors.textSecondary) },
                confirmButton = { TextButton(onClick = { scope.launch { vm.delete(); onClose() } }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            )
        }
    }
}
