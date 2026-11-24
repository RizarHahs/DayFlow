package com.dayflow.app.ui.feature.roadmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.domain.model.RoadmapSlot
import com.dayflow.app.domain.model.SlotStatus
import com.dayflow.app.domain.time.dayLongLabel
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.theme.*

private sealed interface Entry {
    data class Slot(val s: RoadmapSlot) : Entry
    data object Now : Entry
}

@Composable
fun RoadmapScreen(
    bottomPadding: Dp,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    vm: RoadmapViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var actionSlot by remember { mutableStateOf<RoadmapSlot?>(null) }
    var rescheduleSlot by remember { mutableStateOf<RoadmapSlot?>(null) }

    SectionTheme(DaySection.NORMAL) {
        Column(modifier.fillMaxSize().statusBarsPadding()) {
            val s = ui
            Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DAILY ROADMAP", style = DfType.label, color = Df.section.accent)
                    Text(
                        s?.let { dayLongLabel(it.day) } ?: "",
                        style = DfType.title, color = Df.colors.textPrimary,
                    )
                }
                Box(Modifier.size(44.dp).pressable(haptic = null) { vm.shiftDay(-1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronLeft, "Previous day", tint = Df.colors.textPrimary)
                }
                if (s != null && !s.isToday) {
                    Text(
                        "Today", style = DfType.bodySmall, color = Df.section.accent,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Df.section.soft)
                            .pressable { vm.goToday() }.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Box(Modifier.size(44.dp).pressable(haptic = null) { vm.shiftDay(1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronRight, "Next day", tint = Df.colors.textPrimary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp)).border(1.dp, Df.section.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .pressable(onClick = onAdd).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.Add, null, tint = Df.section.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add activity", style = DfType.bodyMedium, color = Df.section.accent)
            }
            Spacer(Modifier.height(8.dp))

            if (s == null) return@Column
            if (s.slots.isEmpty()) {
                Box(Modifier.weight(1f).padding(bottom = bottomPadding), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Rounded.Schedule, "Nothing scheduled", "Add activities from wake-up to sleep. They repeat on the weekdays you choose.")
                }
            } else {
                val entries = buildList<Entry> {
                    var nowPlaced = !s.isToday
                    s.slots.forEach { slot ->
                        if (!nowPlaced && slot.startMinute > s.nowMinute) { add(Entry.Now); nowPlaced = true }
                        add(Entry.Slot(slot))
                    }
                    if (!nowPlaced) add(Entry.Now)
                }
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                ) {
                    items(entries.size) { i ->
                        when (val e = entries[i]) {
                            is Entry.Now -> NowLine(s.nowMinute)
                            is Entry.Slot -> SlotRow(e.s, first = i == 0, last = i == entries.lastIndex) { actionSlot = e.s }
                        }
                    }
                }
            }
        }
    }

    actionSlot?.let { slot ->
        SlotActions(
            slot = slot,
            onDismiss = { actionSlot = null },
            onDone = { vm.toggleDone(slot) },
            onSkip = { vm.skip(slot) },
            onReschedule = { rescheduleSlot = slot },
            onStartTimer = { vm.startTimer(slot) },
            onRestore = { vm.restoreOriginal(slot) },
            onEdit = { onEdit(slot.itemId) },
            onDelete = { vm.deleteActivity(slot) },
        )
    }
    rescheduleSlot?.let { slot ->
        RescheduleDialog(
            slot = slot,
            conflictsFor = { st, en -> vm.conflictsFor(slot, st, en) },
            onDismiss = { rescheduleSlot = null },
            onConfirm = { st, en -> vm.reschedule(slot, st, en); rescheduleSlot = null },
        )
    }
}

@Composable
private fun statusColor(s: SlotStatus): Color = when (s) {
    SlotStatus.DONE -> sectionPalette(DaySection.FOUNDATION, Df.colors.isDark).accent
    SlotStatus.CURRENT -> Df.section.accent
    SlotStatus.UPCOMING -> Df.colors.textTertiary
    SlotStatus.MISSED -> PriorityColors.High
    SlotStatus.SKIPPED -> Df.colors.textTertiary
}

@Composable
private fun NowLine(minute: Int) {
    val c = Df.section.accent
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(formatMinute(minute), style = DfType.mono, color = c, modifier = Modifier.width(52.dp))
        Box(Modifier.size(10.dp).clip(CircleShape).background(c))
        Box(Modifier.weight(1f).height(2.dp).background(c.copy(alpha = 0.6f)))
    }
}

@Composable
private fun SlotRow(slot: RoadmapSlot, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val c = statusColor(slot.status)
    val rail = Df.colors.hairline.copy(alpha = 0.22f)
    val dimmed = slot.status == SlotStatus.SKIPPED
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(52.dp).padding(top = 14.dp)) {
            Text(formatMinute(slot.startMinute), style = DfType.mono, color = Df.colors.textPrimary)
            Text(formatMinute(slot.endMinute), style = DfType.mono, color = Df.colors.textTertiary)
        }
        Box(
            Modifier.width(24.dp).fillMaxHeight().drawBehind {
                val x = size.width / 2
                drawLine(rail, Offset(x, if (first) 22.dp.toPx() else 0f), Offset(x, if (last) 22.dp.toPx() else size.height), strokeWidth = 2.dp.toPx())
            },
        ) {
            Box(
                Modifier.padding(top = 14.dp).size(18.dp).align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .background(if (slot.status == SlotStatus.DONE) c else Df.colors.background)
                    .border(2.dp, c, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (slot.status == SlotStatus.DONE) Icon(Icons.Rounded.Check, null, tint = Df.section.onAccent, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        val shape = RoundedCornerShape(14.dp)
        Column(
            Modifier.weight(1f).padding(vertical = 5.dp).clip(shape)
                .background(if (slot.status == SlotStatus.CURRENT) Df.section.soft else Df.colors.surface)
                .border(1.dp, if (slot.status == SlotStatus.CURRENT) c else Df.colors.hairline, shape)
                .pressable(pressedScale = 0.985f, haptic = null, onClick = onClick)
                .padding(14.dp),
        ) {
            Text(
                slot.title, style = DfType.titleSmall,
                color = if (dimmed || slot.status == SlotStatus.DONE) Df.colors.textTertiary else Df.colors.textPrimary,
                textDecoration = if (dimmed) TextDecoration.LineThrough else null,
            )
            val tags = buildList<Pair<String, Color>> {
                when (slot.status) {
                    SlotStatus.CURRENT -> add("NOW" to c)
                    SlotStatus.MISSED -> add("MISSED" to c)
                    SlotStatus.DONE -> add("DONE" to c)
                    SlotStatus.SKIPPED -> add("SKIPPED" to c)
                    SlotStatus.UPCOMING -> Unit
                }
                if (slot.rescheduled) add("MOVED" to Df.colors.textSecondary)
                if (slot.conflict) add("OVERLAP" to PriorityColors.Critical)
            }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { tags.forEach { (t, col) -> Pill(t, col) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotActions(
    slot: RoadmapSlot,
    onDismiss: () -> Unit,
    onDone: () -> Unit, onSkip: () -> Unit, onReschedule: () -> Unit, onStartTimer: () -> Unit,
    onRestore: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = DfShapes.sheet, containerColor = Df.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Df.colors.textTertiary) },
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp).navigationBarsPadding()) {
            Text(slot.title, style = DfType.title, color = Df.colors.textPrimary)
            Text("${formatMinute(slot.startMinute)} – ${formatMinute(slot.endMinute)}", style = DfType.monoLarge, color = Df.colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            fun act(label: String, destructive: Boolean = false, run: () -> Unit) = Triple(label, destructive, run)
            val actions = buildList {
                add(act(if (slot.status == SlotStatus.DONE) "Mark as not done" else "Mark as done") { onDone() })
                if (slot.status != SlotStatus.DONE && slot.status != SlotStatus.SKIPPED) {
                    add(act("Start timer (${slot.endMinute - slot.startMinute} min)") { onStartTimer() })
                }
                if (slot.status != SlotStatus.SKIPPED) add(act("Skip today") { onSkip() })
                else add(act("Undo skip") { onRestore() })
                add(act("Reschedule for this day") { onReschedule() })
                if (slot.rescheduled) add(act("Restore original time") { onRestore() })
                add(act("Edit activity") { onEdit() })
                add(act("Remove activity", destructive = true) { onDelete() })
            }
            actions.forEach { (label, destructive, run) ->
                Text(
                    label, style = DfType.body,
                    color = if (destructive) PriorityColors.Critical else Df.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .pressable(pressedScale = 0.99f, haptic = null) { onDismiss(); run() }
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun RescheduleDialog(
    slot: RoadmapSlot,
    conflictsFor: (Int, Int) -> List<com.dayflow.app.domain.usecase.TimeBlock>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var start by remember { mutableStateOf<Int?>(slot.startMinute) }
    var end by remember { mutableStateOf<Int?>(slot.endMinute) }
    val valid = start != null && end != null && end!! > start!!
    val clashes = if (valid) conflictsFor(start!!, end!!) else emptyList()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Df.colors.surface,
        title = { Text("Reschedule", style = DfType.title, color = Df.colors.textPrimary) },
        text = {
            Column {
                FieldLabel("Start"); TimeField(start, { start = it }, defaultMinute = slot.startMinute)
                Spacer(Modifier.height(12.dp))
                FieldLabel("End"); TimeField(end, { end = it }, defaultMinute = slot.endMinute)
                if (start != null && end != null && !valid) {
                    Spacer(Modifier.height(10.dp))
                    Text("End must be after start.", style = DfType.bodySmall, color = PriorityColors.Critical)
                }
                if (clashes.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Overlaps with " + clashes.joinToString { "${it.title} (${formatMinute(it.start)})" } + ". You can still save.",
                        style = DfType.bodySmall, color = PriorityColors.High,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(start!!, end!!) }) {
                Text(if (clashes.isEmpty()) "Save" else "Save anyway")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
