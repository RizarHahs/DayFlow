package com.dayflow.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dayflow.app.ui.theme.Df

/**
 * Visual-only completion marker. The surrounding row owns the click (>=48dp target),
 * so this never competes for touches.
 */
@Composable
fun CheckNode(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    shape: Shape = CircleShape,
    color: Color = Df.section.accent,
    onColor: Color = Df.section.onAccent,
) {
    val fill by animateColorAsState(if (checked) color else Color.Transparent, tween(220), label = "fill")
    val border by animateColorAsState(if (checked) color else Df.colors.textTertiary, tween(220), label = "border")
    val tick by animateFloatAsState(
        if (checked) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tick",
    )
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(fill)
            .border(1.5.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = onColor,
            modifier = Modifier
                .size(size * 0.62f)
                .graphicsLayer {
                    scaleX = tick; scaleY = tick; alpha = tick.coerceIn(0f, 1f)
                },
        )
    }
}
