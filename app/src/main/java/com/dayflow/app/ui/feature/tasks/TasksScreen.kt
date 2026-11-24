package com.dayflow.app.ui.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.domain.model.TaskItem
import com.dayflow.app.domain.model.TaskKind
import com.dayflow.app.ui.components.MenuAction
import com.dayflow.app.ui.components.ReorderableColumn
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.feature.home.InlineAdd
import com.dayflow.app.ui.theme.*

@Composable
fun TasksScreen(
    requestedLane: Lane?,
    onLaneShown: () -> Unit,
    bottomPadding: Dp,
    onEditTask: (Long, TaskKind) -> Unit,
    onNewTask: (TaskKind) -> Unit,
    onEditHabit: (Long) -> Unit,
    onNewHabit: () -> Unit,
    modifier: Modifier = Modifier,
    vm: TasksViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var lane by rememberSaveable { mutableStateOf(Lane.FOUNDATION) }
    LaunchedEffect(requestedLane) {
        if (requestedLane != null) { lane = requestedLane; onLaneShown() }
    }

    SectionTheme(lane.section) {
        Column(modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                "Tasks", style = DfType.display, color = Df.colors.textPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
            Spacer(Modifier.height(14.dp))
            LaneTabs(lane, { lane = it }, Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            val s = ui
            if (s == null) return@Column
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (lane) {
                    Lane.FOUNDATION -> foundationLane(s, vm, onEditHabit, onNewHabit)
                    Lane.IMPORTANT -> importantLane(s, vm, onEditTask, onNewTask)
                    Lane.NORMAL -> normalLane(s, vm, onEditTask, onNewTask)
                    Lane.EXTRA -> extraLane(s, vm, onEditTask, onNewTask)
                }
            }
        }
    }
}

@Composable
private fun LaneTabs(selected: Lane, onSelect: (Lane) -> Unit, modifier: Modifier = Modifier) {
    val dark = Df.colors.isDark
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Lane.entries.forEach { l ->
            val on = l == selected
            val accent = sectionPalette(l.section, dark).accent
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) accent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                    .border(1.dp, if (on) accent else Df.colors.hairline, RoundedCornerShape(50))
                    .pressable { onSelect(l) },
                contentAlignment = Alignment.Center,
            ) { Text(l.label, style = DfType.bodySmall, color = if (on) accent else Df.colors.textSecondary) }
        }
    }
}

// ---- shared bits ----

@Composable
private fun SectionHint(text: String) {
    Text(text.uppercase(), style = DfType.label, color = Df.colors.textTertiary, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = DfType.body, color = Df.colors.textSecondary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
}

@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    val p = Df.section
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, p.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Add, null, tint = p.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = DfType.bodyMedium, color = p.accent)
    }
}

// ---- lanes ----

private fun LazyListScope.foundationLane(
    s: TasksUi, vm: TasksViewModel, onEdit: (Long) -> Unit, onNew: () -> Unit,
) {
    val due = s.habits.filter { it.dueToday }
    item {
        Text(
            "${due.count { it.done }} of ${due.size} done today",
            style = DfType.bodyMedium, color = Df.colors.textSecondary, modifier = Modifier.padding(start = 4.dp),
        )
    }
    if (s.habits.isEmpty()) item { EmptyLine("No habits yet. Add the first one you never want to skip.") }
    item {
        ReorderableColumn(
            items = s.habits, idOf = { it.id },
            onReorder = { vm.reorderHabits(it.map { h -> h.id }) },
        ) { habit, handle, _ ->
            Box(Modifier.padding(bottom = 10.dp)) {
                HabitRow(
                    habit = habit,
                    onToggle = { vm.toggleHabit(habit.id) },
                    onOpen = { onEdit(habit.id) },
                    handle = handle,
                    menu = listOf(
                        MenuAction("Edit") { onEdit(habit.id) },
                    ) + (if (habit.timerSeconds != null) listOf(MenuAction("Start timer (${habit.timerSeconds / 60} min)") { vm.startHabitTimer(habit) }) else emptyList()) + listOf(
                        MenuAction("Remove from routine", destructive = true) { vm.archiveHabit(habit.id) },
                    ),
                )
            }
        }
    }
    item { AddButton("New habit", onNew) }
}

private fun LazyListScope.importantLane(
    s: TasksUi, vm: TasksViewModel, onEdit: (Long, TaskKind) -> Unit, onNew: (TaskKind) -> Unit,
) {
    val all = s.lane(TaskKind.IMPORTANT)
    val decide = all.filter { it.overdue && it.rolloverPolicy == com.dayflow.app.domain.model.RolloverPolicy.ASK }
    val rest = (all - decide.toSet()).sortedWith(compareBy<TaskItem> { it.done }.thenBy { it.startMinute ?: Int.MAX_VALUE })
    val upcoming = s.upcoming(TaskKind.IMPORTANT)

    if (decide.isNotEmpty()) {
        item { SectionHint("Needs a decision") }
        items(decide.size) { i ->
            val t = decide[i]
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskRow(t, s.today, { vm.toggleTask(t.id) }, { onEdit(t.id, t.kind) }, taskMenu(t, vm, onEdit))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DecisionButton("Move to today", Modifier.weight(1f)) { vm.moveToToday(t.id) }
                    DecisionButton("Let it go", Modifier.weight(1f)) { vm.drop(t.id) }
                }
            }
        }
    }
    item { SectionHint("Today") }
    if (rest.isEmpty()) item { EmptyLine("Nothing critical today. Add the one thing that deserves serious attention.") }
    items(rest.size) { i ->
        val t = rest[i]
        TaskRow(t, s.today, { vm.toggleTask(t.id) }, { onEdit(t.id, t.kind) }, taskMenu(t, vm, onEdit))
    }
    if (upcoming.isNotEmpty()) {
        item { SectionHint("Upcoming") }
        items(upcoming.size) { i ->
            val t = upcoming[i]
            TaskRow(t, s.today, { vm.toggleTask(t.id) }, { onEdit(t.id, t.kind) }, taskMenu(t, vm, onEdit), showDate = true)
        }
    }
    item { AddButton("New important task") { onNew(TaskKind.IMPORTANT) } }
}

private fun LazyListScope.normalLane(
    s: TasksUi, vm: TasksViewModel, onEdit: (Long, TaskKind) -> Unit, onNew: (TaskKind) -> Unit,
) {
    val all = s.lane(TaskKind.NORMAL)
    val open = all.filter { !it.done }
    val done = all.filter { it.done }
    val upcoming = s.upcoming(TaskKind.NORMAL)

    item { InlineAdd(onAdd = vm::addNormal) }
    if (all.isEmpty()) item { EmptyLine("No targets yet. Type one above and press Enter.") }
    if (open.isNotEmpty()) {
        item {
            ReorderableColumn(
                items = open, idOf = { it.id },
                onReorder = { vm.reorderTasks(it.map { t -> t.id }) },
            ) { t, handle, _ ->
                Box(Modifier.padding(bottom = 10.dp)) {
                    TaskRow(t, s.today, { vm.toggleTask(t.id) }, { onEdit(t.id, t.kind) }, taskMenu(t, vm, onEdit), handle = handle)
                }
            }
        }
    }
    if (done.isNotEmpty()) {
        item { SectionHint("Completed") }
        items(done.size) { i ->
            val t = done[i]
            TaskRow(t, s.today, { vm.toggleTask(t.id) }, { onEdit(t.id, t.kind) }, taskMenu(t, vm, onEdit))
        }
    }
    if (upcoming.isNotEmpty()) {
        item { SectionHint("Upcoming") }
        items(upcoming.size) { i ->
            val t = upcoming[i]
            TaskRow(t, s.today, { vm.toggleTask(t.id) }, { onEdit(t.id, t.kind) }, taskMenu(t, vm, onEdit), showDate = true)
        }
    }
    item { AddButton("New target with details") { onNew(TaskKind.NORMAL) } }
}

private fun LazyListScope.extraLane(
    s: TasksUi, vm: TasksViewModel, onEdit: (Long, TaskKind) -> Unit, onNew: (TaskKind) -> Unit,
) {
    val all = s.lane(TaskKind.EXTRA)
    val open = all.filter { !it.done }
    val done = all.filter { it.done }
    val upcoming = s.upcoming(TaskKind.EXTRA)

    item { Text("Optional. Never counts against your day.", style = DfType.bodyMedium, color = Df.colors.textSecondary, modifier = Modifier.padding(start = 4.dp)) }
    if (all.isEmpty()) item { EmptyLine("Nothing here yet. Add something exciting to try.") }
    items(open.size) { i ->
        val t = open[i]
        TaskRow(t, s.today, { vm.toggleTask(t.id) }, { onEdit(t.id, t.kind) }, taskMenu(t, vm, onEdit))
    }
    if (done.isNotEmpty()) {
        item { SectionHint("Achieved · ${done.size}") }
        items(done.size) { i ->
            val t = done[i]
            TaskRow(t, s.today, { vm.toggleTask(t.id) }, { onEdit(t.id, t.kind) }, taskMenu(t, vm, onEdit))
        }
    }
    if (upcoming.isNotEmpty()) {
        item { SectionHint("Moved to later") }
        items(upcoming.size) { i ->
            val t = upcoming[i]
            TaskRow(t, s.today, { vm.toggleTask(t.id) }, { onEdit(t.id, t.kind) }, taskMenu(t, vm, onEdit), showDate = true)
        }
    }
    item { AddButton("New extra target") { onNew(TaskKind.EXTRA) } }
}

@Composable
private fun DecisionButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(Df.section.soft)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = DfType.bodySmall, color = Df.section.accent) }
}

/** Row actions depend on the lane: promote/demote, move, make habit, drop. */
private fun taskMenu(t: TaskItem, vm: TasksViewModel, onEdit: (Long, TaskKind) -> Unit): List<MenuAction> = buildList {
    add(MenuAction("Edit") { onEdit(t.id, t.kind) })
    val timerMin = t.timerSeconds?.div(60) ?: t.estimateMin
    if (!t.done && timerMin != null) add(MenuAction("Start timer ($timerMin min)") { vm.startTaskTimer(t) })
    if (!t.done) add(MenuAction("Move to tomorrow") { vm.moveToTomorrow(t.id) })
    if (t.overdue) add(MenuAction("Move to today") { vm.moveToToday(t.id) })
    when (t.kind) {
        TaskKind.EXTRA -> {
            add(MenuAction("Promote to Important") { vm.promote(t.id, TaskKind.IMPORTANT) })
            add(MenuAction("Promote to Target") { vm.promote(t.id, TaskKind.NORMAL) })
        }
        TaskKind.NORMAL -> {
            add(MenuAction("Promote to Important") { vm.promote(t.id, TaskKind.IMPORTANT) })
            add(MenuAction("Make it a daily habit") { vm.toHabit(t.id) })
        }
        TaskKind.IMPORTANT -> add(MenuAction("Move to Targets") { vm.promote(t.id, TaskKind.NORMAL) })
    }
    add(MenuAction("Drop it", destructive = true) { vm.drop(t.id) })
}
