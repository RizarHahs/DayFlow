package com.dayflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfShapes

/** Frosted-glass surface: soft shadow, translucent gradient fill, gradient hairline border. */
@Composable
fun Modifier.glass(
    shape: Shape = DfShapes.glass,
    tint: Color = Color.Unspecified,
    raised: Boolean = false,
    chrome: Boolean = false,
): Modifier {
    val g = Df.glass
    val baseTop = if (chrome) g.chromeTop else g.fillTop
    val baseBottom = if (chrome) g.chromeBottom else g.fillBottom
    val top = if (tint == Color.Unspecified) baseTop else tint.copy(alpha = g.tintAlpha).compositeOver(baseTop)
    val bottom = if (tint == Color.Unspecified) baseBottom else tint.copy(alpha = g.tintAlpha * 0.6f).compositeOver(baseBottom)
    val elevation = if (raised) g.shadowRaised else g.shadow
    return this
        .shadow(elevation, shape, ambientColor = g.shadowColor, spotColor = g.shadowColor)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, Brush.verticalGradient(listOf(g.borderTop, g.borderBottom)), shape)
}

/** Glass card. Pass [onClick] to make it tappable with the app's standard press-scale + haptic. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = DfShapes.glass,
    tint: Color = Color.Unspecified,
    raised: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = if (onClick != null) modifier.pressable(pressedScale = 0.98f, haptic = null, onClick = onClick) else modifier
    Column(base.glass(shape, tint, raised).padding(contentPadding), content = content)
}

/** Round glass button (icon actions). Press-scale on the whole button; no haptic by default, like the old bare icons. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .size(size)
            .pressable(haptic = null, onClick = onClick)
            .glass(CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * App-wide backdrop: base colour plus a few static soft colour glows, so glass surfaces have something to frost.
 * Drawn once per frame as three gradient rects; no blur, no animation.
 */
@Composable
fun Modifier.ambientBackground(): Modifier {
    val dark = Df.colors.isDark
    return this
        .background(Df.colors.background)
        .drawBehind {
            val w = size.width
            val h = size.height
            fun glow(color: Color, cx: Float, cy: Float, radius: Float) {
                drawRect(Brush.radialGradient(listOf(color, Color.Transparent), center = Offset(cx, cy), radius = radius))
            }
            if (dark) {
                glow(Color(0x3310B981), 0f, 0f, w * 1.1f)
                glow(Color(0x2E6366F1), w, h * 0.45f, w * 0.95f)
                glow(Color(0x1F0EA5E9), w * 0.15f, h, w * 1.0f)
            } else {
                glow(Color(0x5C8DB8FF), w * 0.9f, 0f, w * 1.1f)
                glow(Color(0x4DFFC09A), 0f, h * 0.42f, w * 0.95f)
                glow(Color(0x4D8FE3CF), w * 0.85f, h, w * 1.0f)
            }
        }
}
