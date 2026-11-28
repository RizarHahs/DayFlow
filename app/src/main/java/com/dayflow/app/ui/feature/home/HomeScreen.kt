package com.dayflow.app.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayflow.app.domain.model.CategoryProgress
import com.dayflow.app.domain.model.RoadmapEntry
import com.dayflow.app.ui.DayUiState
import com.dayflow.app.ui.components.GlassIconButton
import com.dayflow.app.ui.components.ProgressRing
import com.dayflow.app.ui.components.glass
import com.dayflow.app.ui.components.formatMinute
import com.dayflow.app.ui.components.minuteOfDay
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.components.rememberNow
import com.dayflow.app.ui.feature.tasks.Lane
import com.dayflow.app.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Everything Home can do. Built once in the root so screens stay previewable. */
class DayActions(
    val toggleHabit: (Long) -> Unit,
    val toggleTask: (Long) -> Unit,
    val addNormal: (String) -> Unit,
    val togglePin: (Long) -> Unit,
    val captureIdea: () -> Unit,
    val openLane: (Lane) -> Unit,
    val openIdea: (Long) -> Unit,
    val openIdeas: () -> Unit,
    val openAlarms: () -> Unit,
    val openSettings: () -> Unit,
    val openReview: () -> Unit,
    val openMessages: () -> Unit,
)

private val quotes = listOf(
    "Small actions. Consistent progress.",
    "Show up. The rest follows.",
    "Today is a good day to begin again.",
    "One honest step beats a perfect plan.",
    "Quiet consistency builds loud results.",
)

private fun greeting(hour: Int) = when {
    hour < 5 -> "GOOD NIGHT"
    hour < 12 -> "GOOD MORNING"
    hour < 17 -> "GOOD AFTERNOON"
    else -> "GOOD EVENING"
}

@Composable
fun HomeScreen(
    state: DayUiState,
    actions: DayActions,
    bottomPadding: Dp,
    setupAttention: Boolean,
    modifier: Modifier = Modifier,
) {
    val now by rememberNow()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { TopIcons(setupAttention, actions.openMessages, actions.openAlarms, actions.openSettings) }
        item { Header(now, state.score) }
        item { Overview(state) }
        item { NowNext(state.roadmap, now.minuteOfDay()) }
        if (state.dayComplete) {
            item(key = "dayComplete") { Box(Modifier.animateItem()) { DayCompleteBanner() } }
        }
        item { FoundationCard(state.habits, actions.toggleHabit, onOpen = { actions.openLane(Lane.FOUNDATION) }) }
        item { ImportantCard(state.important, actions.toggleTask, onOpen = { actions.openLane(Lane.IMPORTANT) }) }
        item { NormalCard(state.normal, actions.toggleTask, actions.addNormal, onOpen = { actions.openLane(Lane.NORMAL) }) }
        item { ExtraCard(state.extra, state.dayComplete, actions.toggleTask, onOpen = { actions.openLane(Lane.EXTRA) }) }
        item { IdeasCard(state.ideas, actions.togglePin, actions.captureIdea, actions.openIdea, actions.openIdeas) }
        item { ReviewButton(state.dayComplete, actions.openReview) }
    }
}

@Composable
private fun Header(now: LocalDateTime, score: Float) {
    val quote = quotes[now.dayOfYear % quotes.size]
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(greeting(now.hour), style = DfType.label, color = Df.colors.textSecondary)
                Spacer(Modifier.width(10.dp))
                Text(formatMinute(now.minuteOfDay()), style = DfType.monoLarge, color = Df.colors.textTertiary)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
                style = DfType.display, color = Df.colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text("\u201C$quote\u201D", style = DfType.bodySmall, color = Df.colors.textSecondary)
        }
        Spacer(Modifier.width(12.dp))
        ProgressRing(score, Modifier.size(84.dp), strokeWidth = 8.dp, colors = BrandRing) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(score * 100).roundToInt()}", style = DfType.title, color = Df.colors.textPrimary)
                Text("TODAY", style = DfType.label.copy(fontSize = 9.sp), color = Df.colors.textTertiary)
            }
        }
    }
}

@Composable
private fun Overview(s: DayUiState) {
    val dark = Df.colors.isDark
    Row(
        Modifier
            .fillMaxWidth()
            .glass()
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MiniStat("Habits", s.foundation, sectionPalette(DaySection.FOUNDATION, dark).accent)
        MiniStat("Important", s.importantProgress, sectionPalette(DaySection.IMPORTANT, dark).accent)
        MiniStat("Targets", s.normalProgress, sectionPalette(DaySection.NORMAL, dark).accent)
        MiniStat("Extra", s.extraProgress, sectionPalette(DaySection.EXTRA, dark).accent)
    }
}

@Composable
private fun MiniStat(label: String, p: CategoryProgress, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressRing(p.fraction, Modifier.size(48.dp), strokeWidth = 5.dp, colors = listOf(color)) {
            Text("${p.done}/${p.total}", style = DfType.mono.copy(fontSize = 11.sp), color = Df.colors.textPrimary)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = DfType.bodySmall.copy(fontSize = 11.sp), color = Df.colors.textSecondary)
    }
}

/** Current / next activity from the roadmap, using real local time. */
@Composable
private fun NowNext(roadmap: List<RoadmapEntry>, minute: Int) {
    val sorted = roadmap.sortedBy { it.startMinute }
    val current = sorted.firstOrNull { minute >= it.startMinute && minute < it.endMinute }
    val next = sorted.firstOrNull { it.startMinute > minute }
    Row(
        Modifier
            .fillMaxWidth()
            .glass(raised = true)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.3f)) {
            Text("NOW", style = DfType.label, color = sectionPalette(DaySection.FOUNDATION, Df.colors.isDark).accent)
            Spacer(Modifier.height(4.dp))
            if (current != null) {
                Text(current.title, style = DfType.titleSmall, color = Df.colors.textPrimary)
                Text(
                    "${formatMinute(current.startMinute)} – ${formatMinute(current.endMinute)} · ${current.endMinute - minute} min left",
                    style = DfType.mono, color = Df.colors.textSecondary,
                )
            } else {
                Text("Free time", style = DfType.titleSmall, color = Df.colors.textPrimary)
                Text("Nothing scheduled right now", style = DfType.mono, color = Df.colors.textSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(1.dp).height(40.dp).background(Df.colors.hairline))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("NEXT", style = DfType.label, color = Df.colors.textTertiary)
            Spacer(Modifier.height(4.dp))
            if (next != null) {
                Text(next.title, style = DfType.bodyMedium, color = Df.colors.textPrimary, maxLines = 1)
                Text("${formatMinute(next.startMinute)} · in ${next.startMinute - minute}m", style = DfType.mono, color = Df.colors.textSecondary)
            } else {
                Text("Day wrapped", style = DfType.bodyMedium, color = Df.colors.textPrimary)
            }
        }
    }
}

@Composable
private fun DayCompleteBanner() {
    val shape = DfShapes.glass
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF34D399), Color(0xFF38BDF8))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.08f))), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF03261C), modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Day complete", style = DfType.title, color = Color(0xFF03261C))
            Text(
                "Your main responsibilities are done. Extras are pure bonus.",
                style = DfType.bodySmall, color = Color(0xFF03261C).copy(alpha = 0.8f),
            )
        }
    }
}

/** Alarms and Settings entry points. The amber dot means alarm permissions need attention. */
@Composable
private fun TopIcons(attention: Boolean, onMessages: () -> Unit, onAlarms: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(onMessages) {
            Icon(Icons.Rounded.Chat, "Scheduled messages", tint = Df.colors.textPrimary, modifier = Modifier.size(22.dp))
        }
        GlassIconButton(onAlarms) {
            Icon(Icons.Rounded.Alarm, "Alarms", tint = Df.colors.textPrimary, modifier = Modifier.size(22.dp))
        }
        GlassIconButton(onSettings) {
            Icon(Icons.Rounded.Settings, "Settings", tint = Df.colors.textPrimary, modifier = Modifier.size(22.dp))
            if (attention) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 9.dp).size(9.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape).background(PriorityColors.High)
                )
            }
        }
    }
}

/** End-of-day entry. Highlighted once the day is complete, but always available. */
@Composable
private fun ReviewButton(dayComplete: Boolean, onClick: () -> Unit) {
    val c = sectionPalette(DaySection.FOUNDATION, Df.colors.isDark).accent
    val shape = RoundedCornerShape(50)
    val base = Modifier.fillMaxWidth().heightIn(min = 56.dp).pressable(onClick = onClick)
    Row(
        (if (dayComplete) base.clip(shape).background(c) else base.glass(shape)).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            if (dayComplete) "Day complete. Review your day" else "Review your day",
            style = DfType.bodyMedium, color = if (dayComplete) Color(0xFF03261C) else Df.colors.textSecondary,
        )
    }
}
