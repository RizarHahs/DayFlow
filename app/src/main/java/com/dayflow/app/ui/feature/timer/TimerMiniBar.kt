package com.dayflow.app.ui.feature.timer

import com.dayflow.app.ui.components.glass
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dayflow.app.domain.model.TimerSession
import com.dayflow.app.domain.model.TimerState
import com.dayflow.app.ui.components.ProgressRing
import com.dayflow.app.ui.components.formatDuration
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.components.rememberRemainingMs
import com.dayflow.app.ui.theme.*

/** Always-visible timer strip above the bottom bar. Survives navigation because state is persisted. */
@Composable
fun TimerMiniBar(
    session: TimerSession,
    bootCount: Int,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionTheme(DaySection.NORMAL) {
        val remaining = rememberRemainingMs(session, bootCount)
        val finished = session.state == TimerState.FINISHED
        val paused = session.state == TimerState.PAUSED
        val shape = RoundedCornerShape(22.dp)
        Row(
            modifier.fillMaxWidth().height(60.dp)
                .pressable(pressedScale = 0.99f, haptic = null, onClick = onOpen)
                .glass(shape, tint = Df.section.accent, raised = true, chrome = true)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fraction = if (session.durationMs == 0L) 0f else (remaining.toFloat() / session.durationMs).coerceIn(0f, 1f)
            ProgressRing(fraction, Modifier.size(36.dp), strokeWidth = 4.dp) {}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(session.label.ifBlank { "Timer" }, style = DfType.bodySmall, color = Df.colors.textSecondary, maxLines = 1)
                Text(
                    if (finished) "Time's up" else formatDuration(remaining) + if (paused) "  · paused" else "",
                    style = DfType.monoLarge, color = Df.colors.textPrimary,
                )
            }
            Box(
                Modifier.size(52.dp).pressable(haptic = null) { if (finished) onDismiss() else if (paused) onResume() else onPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (finished) Icons.Rounded.Check else if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    if (finished) "Dismiss" else if (paused) "Resume" else "Pause",
                    tint = Df.section.accent, modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}
