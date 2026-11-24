package com.dayflow.app.ui.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.RoadmapDraft
import com.dayflow.app.domain.model.TaskItem
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.RoadmapRepository
import com.dayflow.app.domain.repo.TaskRepository
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.time.maskLabel
import com.dayflow.app.domain.usecase.Conflicts
import com.dayflow.app.domain.usecase.TimeBlock
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoadmapEditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: RoadmapRepository,
    habits: HabitRepository,
    tasks: TaskRepository,
    clock: DayClock,
) : ViewModel() {
    val id: Long = handle.get<Long>("id") ?: 0L
    var draft by mutableStateOf(RoadmapDraft()); private set
    var loaded by mutableStateOf(id == 0L); private set

    val habitOptions: StateFlow<List<Habit>> = habits.observeHabits(clock.today())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val taskOptions: StateFlow<List<TaskItem>> = tasks.observeToday(clock.today())
        .map { l -> l.filter { !it.done } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (id != 0L) viewModelScope.launch {
            repo.getItem(id)?.let {
                draft = RoadmapDraft(it.id, it.title, it.startMinute, it.endMinute, it.repeatMask, it.linkedHabitId, it.linkedTaskId)
            }
            loaded = true
        }
    }

    fun edit(block: (RoadmapDraft) -> RoadmapDraft) { draft = block(draft) }
    suspend fun conflicts(): List<TimeBlock> = Conflicts.forRecurring(draft, repo.activeItems())
    suspend fun save() { repo.save(draft) }
}

@Composable
fun RoadmapEditor(onClose: () -> Unit, modifier: Modifier = Modifier, vm: RoadmapEditorViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val d = vm.draft
    val habits by vm.habitOptions.collectAsStateWithLifecycle()
    val tasks by vm.taskOptions.collectAsStateWithLifecycle()
    var clashes by remember { mutableStateOf<List<TimeBlock>?>(null) }

    SectionTheme(DaySection.NORMAL) {
        if (!vm.loaded) return@SectionTheme
        val valid = d.title.isNotBlank() && d.endMinute > d.startMinute && d.repeatMask != 0
        EditorScaffold(
            title = if (vm.id == 0L) "New activity" else "Edit activity",
            onBack = onClose,
            onSave = {
                scope.launch {
                    val c = vm.conflicts()
                    if (c.isEmpty()) { vm.save(); onClose() } else clashes = c
                }
            },
            saveEnabled = valid,
            modifier = modifier,
        ) {
            FieldLabel("Activity")
            DfTextField(d.title, { v -> vm.edit { it.copy(title = v) } }, "e.g. Morning routine")
            Spacer(Modifier.height(24.dp))
            FieldLabel("Starts")
            TimeField(d.startMinute, { m -> if (m != null) vm.edit { it.copy(startMinute = m) } }, defaultMinute = d.startMinute, clearable = false)
            Spacer(Modifier.height(12.dp))
            FieldLabel("Ends")
            TimeField(d.endMinute, { m -> if (m != null) vm.edit { it.copy(endMinute = m) } }, defaultMinute = d.endMinute, clearable = false)
            if (d.endMinute <= d.startMinute) {
                Text("End must be after start.", style = DfType.bodySmall, color = PriorityColors.Critical, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(24.dp))
            FieldLabel("Repeats · ${maskLabel(d.repeatMask)}")
            WeekdayPicker(d.repeatMask, { m -> vm.edit { it.copy(repeatMask = m) } })

            Spacer(Modifier.height(24.dp))
            FieldLabel("Link to a habit")
            ChoiceChips<Habit?>(
                options = listOf<Habit?>(null) + habits,
                selected = habits.firstOrNull { it.id == d.linkedHabitId },
                label = { it?.title ?: "None" },
                onSelect = { h -> vm.edit { it.copy(linkedHabitId = h?.id) } },
            )
            Text("A linked activity is marked done automatically when the habit is.", style = DfType.bodySmall, color = Df.colors.textTertiary, modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.height(24.dp))
            FieldLabel("Link to a task")
            ChoiceChips<TaskItem?>(
                options = listOf<TaskItem?>(null) + tasks,
                selected = tasks.firstOrNull { it.id == d.linkedTaskId },
                label = { it?.title ?: "None" },
                onSelect = { t -> vm.edit { it.copy(linkedTaskId = t?.id) } },
            )
        }
        clashes?.let { list ->
            AlertDialog(
                onDismissRequest = { clashes = null },
                containerColor = Df.colors.surface,
                title = { Text("Time overlap", style = DfType.title, color = Df.colors.textPrimary) },
                text = {
                    Text(
                        "This overlaps with " + list.joinToString { "${it.title} (${formatMinute(it.start)}–${formatMinute(it.end)})" } + ". Save anyway?",
                        style = DfType.body, color = Df.colors.textSecondary,
                    )
                },
                confirmButton = { TextButton(onClick = { scope.launch { vm.save(); onClose() } }) { Text("Save anyway") } },
                dismissButton = { TextButton(onClick = { clashes = null }) { Text("Adjust") } },
            )
        }
    }
}
