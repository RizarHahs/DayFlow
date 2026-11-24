package com.dayflow.app.ui.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.domain.model.DayDetail
import com.dayflow.app.domain.model.DetailItem
import com.dayflow.app.domain.time.dayLabel
import com.dayflow.app.domain.time.dayLongLabel
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ReviewScreen(onBack: () -> Unit, modifier: Modifier = Modifier, vm: ReviewViewModel = hiltViewModel()) {
    val day by vm.day.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    val dark = Df.colors.isDark
    SectionTheme(DaySection.FOUNDATION) {
        Column(modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).pressable(haptic = null, onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = Df.colors.textPrimary)
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("DAILY REVIEW", style = DfType.label, color = Df.section.accent)
                    Text(dayLabel(day, vm.today) + " · " + dayLongLabel(day), style = DfType.bodySmall, color = Df.colors.textSecondary, maxLines = 1)
                }
                Box(Modifier.size(44.dp).pressable(haptic = null) { vm.shift(-1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronLeft, "Previous day", tint = Df.colors.textPrimary)
                }
                val canNext = day < vm.today
                Box(Modifier.size(44.dp).pressable(enabled = canNext, haptic = null) { vm.shift(1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronRight, "Next day", tint = if (canNext) Df.colors.textPrimary else Df.colors.textTertiary.copy(alpha = 0.4f))
                }
            }
            val d = detail ?: return@Column
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Hero(d, isToday = day == vm.today)
                Categories(d)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniFact("Focus time", formatTracked(d.summary.trackedMs), Modifier.weight(1f))
                    MiniFact("Ideas captured", d.summary.ideasCaptured.toString(), Modifier.weight(1f))
                }
                ItemList("Habits", d.habits, sectionPalette(DaySection.FOUNDATION, dark).accent)
                ItemList("Important", d.important, sectionPalette(DaySection.IMPORTANT, dark).accent)
                ItemList("Targets", d.normal, sectionPalette(DaySection.NORMAL, dark).accent)
                ItemList("Extra", d.extra, sectionPalette(DaySection.EXTRA, dark).accent)
                if (d.ideas.isNotEmpty()) IdeasList(d.ideas)

                Column {
                    FieldLabel("How did the day go?")
                    DfTextField(vm.note, vm::onNoteChange, "A line or two for future you. Saved automatically.", singleLine = false, minLines = 3)
                }
            }
        }
    }
}

private fun formatTracked(ms: Long): String {
    val m = ms / 60_000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

@Composable
private fun Hero(d: DayDetail, isToday: Boolean) {
    val s = d.summary
    val accent = Df.section.accent
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (s.dayComplete) accent.copy(alpha = 0.14f) else Df.colors.surface)
            .border(1.dp, if (s.dayComplete) accent.copy(alpha = 0.5f) else Df.colors.hairline, shape).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(s.score, Modifier.size(96.dp), strokeWidth = 10.dp, colors = BrandRing) {
            Text("${(s.score * 100).roundToInt()}%", style = DfType.title, color = Df.colors.textPrimary)
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            if (s.dayComplete) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Day complete", style = DfType.title, color = Df.colors.textPrimary)
                }
                Text("Your main responsibilities were done.", style = DfType.bodySmall, color = Df.colors.textSecondary)
            } else if (s.hasData) {
                Text(if (isToday) "Day in progress" else "Progress made", style = DfType.title, color = Df.colors.textPrimary)
                Text(
                    if (isToday) "There's still time. Anything left rolls forward on its own." else "Whatever wasn't finished moved forward. Nothing was lost.",
                    style = DfType.bodySmall, color = Df.colors.textSecondary,
                )
            } else {
                Text("A quiet day", style = DfType.title, color = Df.colors.textPrimary)
                Text("Nothing was planned or recorded.", style = DfType.bodySmall, color = Df.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun Categories(d: DayDetail) {
    val s = d.summary
    val dark = Df.colors.isDark
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Df.colors.surface).border(1.dp, Df.colors.hairline, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CategoryRow("Daily foundation", s.habitsDone, s.habitsTotal, sectionPalette(DaySection.FOUNDATION, dark).accent)
        CategoryRow("Important tasks", s.importantDone, s.importantTotal, sectionPalette(DaySection.IMPORTANT, dark).accent)
        CategoryRow("Normal targets", s.normalDone, s.normalTotal, sectionPalette(DaySection.NORMAL, dark).accent)
        CategoryRow("Extra (bonus)", s.extraDone, s.extraTotal, sectionPalette(DaySection.EXTRA, dark).accent)
    }
}

@Composable
private fun CategoryRow(label: String, done: Int, total: Int, color: Color) {
    Column {
        Row {
            Text(label, style = DfType.bodyMedium, color = Df.colors.textPrimary, modifier = Modifier.weight(1f))
            Text(if (total == 0) "none planned" else "$done / $total", style = DfType.mono, color = Df.colors.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Df.colors.hairline)) {
            if (total > 0) Box(Modifier.fillMaxWidth(done.toFloat() / total).fillMaxHeight().background(color))
        }
    }
}

@Composable
private fun MiniFact(label: String, value: String, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.clip(shape).background(Df.colors.surface).border(1.dp, Df.colors.hairline, shape).padding(16.dp)) {
        Text(label.uppercase(), style = DfType.label, color = Df.colors.textTertiary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = DfType.title, color = Df.colors.textPrimary)
    }
}

/** Done items first with a filled check; open items below with a quiet outline (no warning colours). */
@Composable
private fun ItemList(title: String, items: List<DetailItem>, accent: Color) {
    if (items.isEmpty()) return
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Df.colors.surface).border(1.dp, Df.colors.hairline, shape).padding(16.dp)) {
        Text(title.uppercase(), style = DfType.label, color = accent)
        Spacer(Modifier.height(8.dp))
        items.sortedByDescending { it.done }.forEach { it ->
            Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                CheckNode(it.done, size = 20.dp, color = accent)
                Spacer(Modifier.width(12.dp))
                Text(it.title, style = DfType.body, color = if (it.done) Df.colors.textPrimary else Df.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun IdeasList(ideas: List<String>) {
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 6.dp, bottomStart = 18.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Paper.Sheet).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("IDEAS CAPTURED", style = DfType.label, color = Paper.InkSoft)
        ideas.forEach { Text(it, style = DfType.serifBody, color = Paper.Ink) }
    }
}
