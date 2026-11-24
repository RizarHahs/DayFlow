package com.dayflow.app.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dayflow.app.domain.model.Idea
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.Paper
import com.dayflow.app.ui.theme.DfType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val dateFmt = DateTimeFormatter.ofPattern("MMM d")

private fun stamp(ms: Long): String {
    val zdt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return if (zdt.toLocalDate() == LocalDate.now()) zdt.format(timeFmt) else zdt.format(dateFmt)
}

/** CREATIVITY: a paper notebook. Cream sheet, ruled lines, margin, serif type, taped pinned note. */
@Composable
fun IdeasCard(
    ideas: List<Idea>,
    onTogglePin: (Long) -> Unit,
    onCapture: () -> Unit,
    onOpenIdea: (Long) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomEnd = 8.dp, bottomStart = 24.dp)
    val visible = ideas.sortedWith(compareByDescending<Idea> { it.pinned }.thenByDescending { it.createdAt }).take(4)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Paper.Sheet)
            .drawBehind {
                // Ruled lines + red-ish margin, drawn once for the whole sheet.
                var y = 88.dp.toPx()
                while (y < size.height) {
                    drawLine(Paper.Rule, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    y += 28.dp.toPx()
                }
                drawLine(Paper.Margin, Offset(36.dp.toPx(), 0f), Offset(36.dp.toPx(), size.height), strokeWidth = 1.dp.toPx())
            }
            .padding(start = 52.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("IDEAS OF THE DAY", style = DfType.label, color = Paper.InkSoft)
                Text("Notebook", style = DfType.serifTitle, color = Paper.Ink)
            }
            Text("${ideas.size} notes", style = DfType.serifSmall, color = Paper.InkSoft)
            Box(Modifier.size(40.dp).pressable(onClick = onOpenAll), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChevronRight, "Open notebook", tint = Paper.InkSoft, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

        if (visible.isEmpty()) {
            Text("A blank page. Capture the first thought before it slips away.", style = DfType.serifBody, color = Paper.InkSoft)
        }
        visible.forEach { NoteRow(it, onTogglePin, onOpenIdea) }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(50))
                .background(Paper.Ink)
                .pressable(onClick = onCapture)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.Lightbulb, null, tint = Paper.Sheet, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Capture an idea", style = DfType.bodyMedium, color = Paper.Sheet)
        }
    }
}

@Composable
private fun NoteRow(idea: Idea, onTogglePin: (Long) -> Unit, onOpen: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            Modifier
                .weight(1f)
                .pressable(pressedScale = 0.99f, haptic = null) { onOpen(idea.id) }
                .then(
                    if (idea.pinned) Modifier
                        .graphicsLayer { rotationZ = -1.1f }
                        .clip(RoundedCornerShape(6.dp))
                        .background(Paper.Lavender)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                    else Modifier
                )
        ) {
            if (idea.pinned) {
                // Washi tape strip.
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .offset(y = (-14).dp)
                        .size(width = 44.dp, height = 12.dp)
                        .graphicsLayer { rotationZ = 2f }
                        .background(Paper.Tape)
                )
            }
            Text(idea.text, style = DfType.serifBody, color = Paper.Ink)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stamp(idea.createdAt), style = DfType.serifSmall, color = Paper.InkSoft)
                idea.tags.firstOrNull()?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "#$it",
                        style = DfType.serifSmall,
                        color = Paper.Ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Paper.Lavender.copy(alpha = if (idea.pinned) 0.0f else 0.7f))
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                }
            }
        }
        // 48dp pin target.
        Box(
            Modifier.size(48.dp).pressable(onClick = { onTogglePin(idea.id) }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (idea.pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (idea.pinned) "Unpin" else "Pin",
                tint = if (idea.pinned) Paper.Ink else Paper.InkSoft,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
