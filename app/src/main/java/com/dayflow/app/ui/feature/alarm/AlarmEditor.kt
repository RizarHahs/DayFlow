package com.dayflow.app.ui.feature.alarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.*
import com.dayflow.app.domain.repo.AlarmController
import com.dayflow.app.domain.repo.HabitRepository
import com.dayflow.app.domain.repo.RoadmapRepository
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.time.maskLabel
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
class AlarmEditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val controller: AlarmController,
    habits: HabitRepository,
    roadmap: RoadmapRepository,
    clock: DayClock,
) : ViewModel() {
    val id: Long = handle.get<Long>("id") ?: 0L
    var draft by mutableStateOf(AlarmDraft()); private set
    var loaded by mutableStateOf(id == 0L); private set

    val habitOptions: StateFlow<List<Habit>> = habits.observeHabits(clock.today())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activityOptions: StateFlow<List<RoadmapItem>> = roadmap.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (id != 0L) viewModelScope.launch {
            controller.get(id)?.let {
                draft = AlarmDraft(
                    it.id, it.minute, it.repeatMask, it.label, it.kind, it.soundUri, it.vibrate,
                    it.snoozeMin, it.linkedHabitId, it.linkedRoadmapId, it.enabled,
                )
            }
            loaded = true
        }
    }

    fun edit(block: (AlarmDraft) -> AlarmDraft) { draft = block(draft) }
    suspend fun save() { controller.save(draft.copy(enabled = true)) }
    suspend fun delete() { controller.delete(id) }
}

private fun soundTitle(ctx: Context, uri: String?): String {
    if (uri == null) return "Default alarm sound"
    return try { RingtoneManager.getRingtone(ctx, Uri.parse(uri))?.getTitle(ctx) ?: "Custom sound" } catch (_: Exception) { "Custom sound" }
}

@Composable
fun AlarmEditor(onClose: () -> Unit, modifier: Modifier = Modifier, vm: AlarmEditorViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val d = vm.draft
    val habits by vm.habitOptions.collectAsStateWithLifecycle()
    val activities by vm.activityOptions.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val uri = r.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
            vm.edit { it.copy(soundUri = uri?.toString()) }   // null = user chose "Default"
        }
    }

    SectionTheme(DaySection.IMPORTANT) {
        if (!vm.loaded) return@SectionTheme
        EditorScaffold(
            title = if (vm.id == 0L) "New alarm" else "Edit alarm",
            onBack = onClose,
            onSave = { scope.launch { vm.save(); onClose() } },
            saveEnabled = true,
            modifier = modifier,
        ) {
            NotificationHint()
            Spacer(Modifier.height(16.dp))
            FieldLabel("Time")
            TimeField(d.minute, { m -> if (m != null) vm.edit { it.copy(minute = m) } }, defaultMinute = d.minute, clearable = false)
            Spacer(Modifier.height(24.dp))

            FieldLabel("Repeats · " + if (d.repeatMask == 0) "Once" else maskLabel(d.repeatMask))
            WeekdayPicker(d.repeatMask, { m -> vm.edit { it.copy(repeatMask = m) } })
            Text("Select no days for a one-time alarm.", style = DfType.bodySmall, color = Df.colors.textTertiary, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(24.dp))

            FieldLabel("Label")
            DfTextField(d.label, { v -> vm.edit { it.copy(label = v) } }, "e.g. Wake up")
            Spacer(Modifier.height(24.dp))

            FieldLabel("Type")
            ChoiceChips(AlarmKind.entries.toList(), d.kind, { it.label }, { k -> vm.edit { it.copy(kind = k) } })
            Spacer(Modifier.height(24.dp))

            FieldLabel("Link to a habit")
            ChoiceChips<Habit?>(
                options = listOf<Habit?>(null) + habits,
                selected = habits.firstOrNull { it.id == d.linkedHabitId },
                label = { it?.title ?: "None" },
                onSelect = { h -> vm.edit { it.copy(linkedHabitId = h?.id) } },
            )
            Spacer(Modifier.height(24.dp))

            FieldLabel("Link to a roadmap activity")
            ChoiceChips<RoadmapItem?>(
                options = listOf<RoadmapItem?>(null) + activities,
                selected = activities.firstOrNull { it.id == d.linkedRoadmapId },
                label = { it?.title ?: "None" },
                onSelect = { a -> vm.edit { it.copy(linkedRoadmapId = a?.id) } },
            )
            Spacer(Modifier.height(24.dp))

            FieldLabel("Sound")
            ValueRow(
                text = soundTitle(ctx, d.soundUri), set = d.soundUri != null,
                onClick = {
                    picker.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, d.soundUri?.let { Uri.parse(it) })
                    )
                },
                onClear = if (d.soundUri != null) ({ vm.edit { it.copy(soundUri = null) } }) else null,
            )
            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Vibrate", style = DfType.bodyMedium, color = Df.colors.textPrimary)
                    Text("Vibrate while the alarm rings", style = DfType.bodySmall, color = Df.colors.textTertiary)
                }
                Switch(
                    checked = d.vibrate, onCheckedChange = { v -> vm.edit { it.copy(vibrate = v) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Df.section.onAccent, checkedTrackColor = Df.section.accent,
                        uncheckedThumbColor = Df.colors.textSecondary, uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = Df.colors.textTertiary,
                    ),
                )
            }
            Spacer(Modifier.height(24.dp))

            FieldLabel("Snooze")
            ChoiceChips(listOf(5, 9, 10, 15), d.snoozeMin, { "$it min" }, { m -> vm.edit { it.copy(snoozeMin = m) } })

            if (vm.id != 0L) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Delete alarm", style = DfType.bodyMedium, color = PriorityColors.Critical,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp))
                        .pressable { scope.launch { vm.delete(); onClose() } }.padding(vertical = 14.dp),
                )
            }
        }
    }
}
