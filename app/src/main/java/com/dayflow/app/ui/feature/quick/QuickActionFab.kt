package com.dayflow.app.ui.feature.quick

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfType
import com.dayflow.app.ui.theme.sectionPalette

/**
 * Global quick-action button. Tap expands a menu (nearest = Idea, the most frequent capture).
 * If [directKind] is set (e.g. on the Ideas tab) one tap opens that capture sheet immediately.
 */
@Composable
fun QuickActionFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onPick: (QuickKind) -> Unit,
    onTimer: () -> Unit,
    onAlarm: () -> Unit,
    onMessage: () -> Unit,
    directKind: QuickKind?,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, tween(220), label = "fabRotate")
    val dark = Df.colors.isDark

    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 4 } + scaleIn(tween(220), initialScale = 0.9f),
            exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 4 } + scaleOut(tween(160), targetScale = 0.9f),
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Far to near: Alarm, Timer, Habit, Important, Normal, Extra, Idea (idea = one thumb-reach away).
                QuickRow("WhatsApp message", Icons.Rounded.Chat, Df.colors.raised, Df.colors.textPrimary, onMessage)
                QuickRow("Alarm", Icons.Rounded.Alarm, Df.colors.raised, Df.colors.textPrimary, onAlarm)
                QuickRow("Timer", Icons.Rounded.Timer, Df.colors.raised, Df.colors.textPrimary, onTimer)
                QuickKind.entries.forEach { kind ->
                    val p = sectionPalette(kind.section, dark)
                    QuickRow(kind.label, kind.icon, p.accent, p.onAccent) { onPick(kind) }
                }
            }
        }

        Box(
            Modifier
                .size(60.dp)
                .shadow(12.dp, CircleShape, spotColor = Color(0xFF34D399))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF10B981))))
                .pressable(pressedScale = 0.93f) {
                    if (directKind != null && !expanded) onPick(directKind) else onToggle()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = if (expanded) "Close quick actions" else "Quick add",
                tint = Color(0xFF03261C),
                modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun QuickRow(label: String, icon: ImageVector, bg: Color, fg: Color, onClick: () -> Unit) {
    Row(Modifier.pressable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = DfType.bodyMedium,
            color = Df.colors.textPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Df.colors.raised)
                .border(1.dp, Df.colors.hairline, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(bg).border(1.dp, Df.colors.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = fg, modifier = Modifier.size(22.dp)) }
    }
}
