package com.dayflow.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dayflow.app.ui.theme.Df

/**
 * Animated circular progress. One colour = solid; several = sweep gradient along the arc.
 * Size is controlled by the caller's modifier.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    colors: List<Color> = listOf(Df.section.accent),
    trackColor: Color = Df.colors.hairline,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "ring",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = strokeWidth.toPx()
            val topLeft = Offset(s / 2, s / 2)
            val arc = Size(size.width - s, size.height - s)
            val style = Stroke(width = s, cap = StrokeCap.Round)
            drawArc(trackColor, 0f, 360f, false, topLeft, arc, style = style)
            if (animated > 0.001f) {
                rotate(-90f) {
                    if (colors.size == 1) {
                        drawArc(colors[0], 0f, 360f * animated, false, topLeft, arc, style = style)
                    } else {
                        drawArc(
                            brush = Brush.sweepGradient(colors, center = center),
                            startAngle = 0f, sweepAngle = 360f * animated, useCenter = false,
                            topLeft = topLeft, size = arc, style = style,
                        )
                    }
                }
            }
        }
        content()
    }
}
