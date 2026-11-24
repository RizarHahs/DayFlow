package com.dayflow.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Radial particle burst; fires whenever [trigger] changes to a value > 0. Used by Extra completions. */
@Composable
fun SparkleBurst(trigger: Int, color: Color, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
    }
    Canvas(modifier) {
        val p = progress.value
        if (p < 1f) {
            val maxR = size.minDimension / 2f
            for (i in 0 until 10) {
                val a = (i * 2 * PI / 10).toFloat()
                val r = maxR * (0.35f + 0.65f * p)
                val at = Offset(center.x + cos(a) * r, center.y + sin(a) * r)
                drawCircle(color.copy(alpha = 1f - p), radius = 3.dp.toPx() * (1f - p * 0.6f), center = at)
            }
        }
    }
}
