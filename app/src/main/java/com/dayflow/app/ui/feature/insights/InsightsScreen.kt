package com.dayflow.app.ui.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.domain.model.DaySummary
import com.dayflow.app.domain.model.DotState
import com.dayflow.app.domain.model.HabitHistory
import com.dayflow.app.domain.time.CalendarMath
import com.dayflow.app.domain.time.toLocalDate
import com.dayflow.app.ui.components.EmptyState
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun formatTracked(ms: Long): String {
    val m = ms / 60_000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

@Composable
fun InsightsScreen(
    bottomPadding: Dp,
    onOpenDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
    vm: InsightsViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val mode by vm.currentMode.collectAsStateWithLifecycle()
    SectionTheme(DaySection.FOUNDATION) {
        Column(modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PROGRESS OVER TIME", style = DfType.label, color = Df.section.accent)
                    Text("Insights", style = DfType.display, color = Df.colors.textPrimary)
                }
                Text(
                    "Review today", style = DfType.bodySmall, color = Df.section.onAccent,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Df.section.accent)
                        .pressable { ui?.today?.let(onOpenDay) }.padding(horizontal = 16.dp, vertical = 11.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightsMode.entries.forEach { m ->
                    val on = m == mode
                    Box(
                        Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(50))
                            .background(if (on) Df.section.soft else Color.Transparent)
                            .border(1.dp, if (on) Df.section.accent else Df.colors.hairline, RoundedCornerShape(50))
                            .pressable { vm.setMode(m) },
                        contentAlignment = Alignment.Center,
                    ) { Text(m.label, style = DfType.bodySmall, color = if (on) Df.section.accent else Df.colors.textSecondary) }
                }
            }
            val s = ui ?: return@Column
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (s.mode) {
                    InsightsMode.WEEK -> {
                        item { PeriodNav(weekTitle(s), s.canGoForward, vm::back, vm::forward, vm::thisPeriod) }
                        item { WeekBars(s, onOpenDay) }
                        item { StatGrid(s) }
                        if (s.summaries.values.none { it.hasData }) item { QuietHint() }
                    }
                    InsightsMode.MONTH -> {
                        item { PeriodNav(CalendarMath.monthTitle(s.anchor), s.canGoForward, vm::back, vm::forward, vm::thisPeriod) }
                        item { MonthGrid(s, onOpenDay) }
                        item { StatGrid(s) }
                        if (s.summaries.values.none { it.hasData }) item { QuietHint() }
                    }
                    InsightsMode.HABITS -> {
                        if (s.habits.isEmpty()) item {
                            Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                                EmptyState(Icons.Rounded.LocalFireDepartment, "No habit history yet", "Add a habit and complete it; its consistency and streaks appear here.")
                            }
                        }
                        items(s.habits, key = { it.id }) { HabitCard(it) }
                    }
                }
            }
        }
    }
}

private fun weekTitle(s: InsightsUi): String =
    s.from.toLocalDate().format(shortDate) + " – " + s.to.toLocalDate().format(shortDate)

@Composable
private fun PeriodNav(title: String, canForward: Boolean, onBack: () -> Unit, onForward: () -> Unit, onNow: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).pressable(haptic = null, onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ChevronLeft, "Previous", tint = Df.colors.textPrimary)
        }
        Text(title, style = DfType.titleSmall, color = Df.colors.textPrimary, modifier = Modifier.weight(1f).pressable(haptic = null, onClick = onNow), maxLines = 1)
        Box(Modifier.size(44.dp).pressable(enabled = canForward, haptic = null, onClick = onForward), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ChevronRight, "Next", tint = if (canForward) Df.colors.textPrimary else Df.colors.textTertiary.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(Df.colors.surface).border(1.dp, Df.colors.hairline, shape).padding(16.dp), content = content)
}

@Composable
private fun WeekBars(s: InsightsUi, onOpenDay: (Int) -> Unit) {
    val labels = CalendarMath.weekdayLabels(s.weekStart)
    val accent = Df.section.accent
    Card {
        Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            for (i in 0 until 7) {
                val day = s.from + i
                val sum = s.summaries[day]
                val isToday = day == s.today
                val future = day > s.today
                Column(
                    Modifier.weight(1f).fillMaxHeight().pressable(enabled = !future, pressedScale = 0.96f, haptic = null) { onOpenDay(day) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom,
                ) {
                    if (sum?.dayComplete == true) {
                        Icon(Icons.Rounded.Check, "Day complete", tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.height(2.dp))
                    }
                    val frac = if (sum?.hasData == true) sum.score.coerceIn(0f, 1f) else 0f
                    val barH = (110.dp * frac).coerceAtLeast(6.dp)
                    Box(
                        Modifier.width(22.dp).height(barH).clip(RoundedCornerShape(8.dp))
                            .background(if (frac > 0f) accent.copy(alpha = if (sum?.dayComplete == true) 1f else 0.55f) else Df.colors.hairline),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        labels[i], style = DfType.bodySmall.copy(fontSize = 11.sp),
                        color = if (isToday) accent else if (future) Df.colors.textTertiary.copy(alpha = 0.5f) else Df.colors.textSecondary,
                    )
                    Text(day.toLocalDate().dayOfMonth.toString(), style = DfType.mono.copy(fontSize = 11.sp), color = if (isToday) accent else Df.colors.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(s: InsightsUi, onOpenDay: (Int) -> Unit) {
    val cells = CalendarMath.monthGrid(s.anchor, s.weekStart)
    val labels = CalendarMath.weekdayLabels(s.weekStart)
    val accent = Df.section.accent
    Card {
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { Text(it.take(2), style = DfType.label.copy(fontSize = 10.sp), color = Df.colors.textTertiary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        Spacer(Modifier.height(8.dp))
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val sum = s.summaries[day]
                            val future = day > s.today
                            val isToday = day == s.today
                            val bg = when {
                                sum?.hasData == true -> accent.copy(alpha = 0.14f + 0.62f * sum.score.coerceIn(0f, 1f))
                                else -> Color.Transparent
                            }
                            Box(
                                Modifier.fillMaxSize().clip(CircleShape).background(bg)
                                    .border(if (isToday) 1.5.dp else 0.dp, if (isToday) Df.colors.textPrimary else Color.Transparent, CircleShape)
                                    .pressable(enabled = !future, pressedScale = 0.92f, haptic = null) { onOpenDay(day) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.toLocalDate().dayOfMonth.toString(), style = DfType.bodySmall,
                                    color = when {
                                        sum?.dayComplete == true -> Df.section.onAccent
                                        future -> Df.colors.textTertiary.copy(alpha = 0.45f)
                                        else -> Df.colors.textPrimary
                                    },
                                )
                                if (sum?.dayComplete == true) {
                                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp).clip(CircleShape).background(Df.section.onAccent))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Darker = a fuller day. A dot marks a completed day. Empty days are simply quiet.", style = DfType.bodySmall, color = Df.colors.textTertiary)
    }
}

@Composable
private fun StatGrid(s: InsightsUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Average day", s.averageScorePercent?.let { "$it%" } ?: "–", "of what you planned", Modifier.weight(1f))
            Stat("Days completed", s.completeDays.toString(), "core goals all done", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Habits kept", if (s.habitsTotal == 0) "–" else "${s.habitsDone}/${s.habitsTotal}", "check-ins", Modifier.weight(1f))
            Stat("Focus time", formatTracked(s.trackedMs), "from timers", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Tasks done", s.tasksDone.toString(), "all lanes", Modifier.weight(1f))
            Stat("Ideas captured", s.ideas.toString(), "in your notebook", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String, modifier: Modifier) {
    Card(modifier) {
        Text(label.uppercase(), style = DfType.label, color = Df.colors.textTertiary)
        Spacer(Modifier.height(6.dp))
        Text(value, style = DfType.title.copy(fontSize = 26.sp), color = Df.colors.textPrimary)
        Text(sub, style = DfType.bodySmall, color = Df.colors.textSecondary)
    }
}

@Composable
private fun QuietHint() {
    Text(
        "Nothing recorded in this period yet. That's fine: every day you use DAYFLOW fills this in.",
        style = DfType.body, color = Df.colors.textSecondary, modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun HabitCard(h: HabitHistory) {
    val accent = Df.section.accent
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(h.title, style = DfType.titleSmall, color = if (h.archived) Df.colors.textSecondary else Df.colors.textPrimary)
                Text(
                    (if (h.archived) "Removed from routine · " else "") + "${h.stats.doneDays} of ${h.stats.dueDays} days in the last 30",
                    style = DfType.bodySmall, color = Df.colors.textSecondary,
                )
            }
            Text("${h.stats.percent}%", style = DfType.title, color = accent)
        }
        Spacer(Modifier.height(12.dp))
        h.dots.chunked(14).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { d ->
                    Box(
                        Modifier.size(15.dp).clip(CircleShape).background(
                            when (d) {
                                DotState.DONE -> accent
                                DotState.OPEN -> Color.Transparent
                                DotState.MISSED -> Df.colors.hairline.copy(alpha = 0.3f)
                                DotState.NOT_DUE -> Color.Transparent
                            }
                        ).border(1.dp, if (d == DotState.OPEN) accent else if (d == DotState.NOT_DUE) Df.colors.hairline.copy(alpha = 0.18f) else Color.Transparent, CircleShape)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${h.stats.current} day streak", style = DfType.mono, color = Df.colors.textPrimary)
            }
            Text("best ${h.stats.best}", style = DfType.mono, color = Df.colors.textTertiary)
        }
    }
}
