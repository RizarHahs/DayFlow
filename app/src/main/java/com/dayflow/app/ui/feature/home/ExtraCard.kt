package com.dayflow.app.ui.feature.home

import com.dayflow.app.ui.components.doneState
import com.dayflow.app.ui.components.glass
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dayflow.app.domain.model.TaskItem
import com.dayflow.app.ui.components.CheckNode
import com.dayflow.app.ui.components.OpenChevron
import com.dayflow.app.ui.components.SparkleBurst
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.*

/**
 * OPPORTUNITY: violet-to-magenta gradient, roomy rounded rows, badge shelf, sparkle burst on completion.
 * Always optional. It only changes copy (not rank) when the core day is complete.
 */
@Composable
fun ExtraCard(
    extras: List<TaskItem>,
    coreDone: Boolean,
    onToggle: (Long) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionTheme(DaySection.EXTRA) {
        val p = Df.section
        val done = extras.count { it.done }
        Column(
            modifier
                .fillMaxWidth()
                .glass(tint = p.accent)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = ExtraGradient[1], modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("EXTRA TARGETS", style = DfType.label, color = p.accent, modifier = Modifier.weight(1f))
                Text("+$done BONUS", style = DfType.label, color = ExtraGradient[1])
                OpenChevron(onOpen)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (coreDone) "Everything important is handled." else "Optional. Whenever you're ready.",
                style = DfType.title, color = Df.colors.textPrimary,
            )
            Text(
                if (coreDone) "Now, what else can you accomplish?" else "Bonus achievements never count against your day.",
                style = DfType.bodySmall, color = Df.colors.textSecondary,
            )
            Spacer(Modifier.height(14.dp))

            if (extras.isEmpty()) {
                Text("Add something exciting to try.", style = DfType.body, color = Df.colors.textSecondary)
            }
            extras.sortedBy { it.done }.forEach {
                ExtraRow(it, onToggle)
                Spacer(Modifier.height(8.dp))
            }

            val badges = buildList {
                if (done >= 1) add(Badge("Spark", Icons.Rounded.Bolt))
                if (done >= 2) add(Badge("Explorer", Icons.Rounded.Star))
                if (done >= 3) add(Badge("Trailblazer", Icons.Rounded.EmojiEvents))
            }
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    badges.forEach { BadgeChip(it) }
                }
            }
        }
    }
}

private data class Badge(val name: String, val icon: ImageVector)

@Composable
private fun BadgeChip(b: Badge) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(ExtraGradient.map { it.copy(alpha = 0.22f) }))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(b.icon, null, tint = ExtraGradient[1], modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(b.name, style = DfType.bodySmall, color = Df.colors.textPrimary)
    }
}

@Composable
private fun ExtraRow(t: TaskItem, onToggle: (Long) -> Unit) {
    var burst by remember { mutableIntStateOf(0) }
    val rowShape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(rowShape)
            .background(Df.colors.textPrimary.copy(alpha = if (t.done) 0.04f else 0.07f))
            .doneState(t.done)
            .pressable(pressedScale = 0.98f, haptic = HapticFeedbackType.LongPress) {
                if (!t.done) burst++ // celebrate only on completing, not on undo
                onToggle(t.id)
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            SparkleBurst(burst, ExtraGradient[1], Modifier.requiredSize(84.dp))
            CheckNode(t.done, size = 28.dp, shape = CircleShape, color = ExtraGradient[0])
        }
        Spacer(Modifier.width(14.dp))
        Text(
            t.title,
            style = DfType.bodyMedium,
            color = if (t.done) Df.colors.textTertiary else Df.colors.textPrimary,
            textDecoration = if (t.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        t.estimateMin?.let { Text("${it}m", style = DfType.mono, color = Df.colors.textSecondary) }
    }
}
