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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
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
    val spread = if (raised) g.shadowRaised else g.shadow
    val shadowAlpha = if (raised) g.shadowAlpha * 1.25f else g.shadowAlpha
    return this
        .softShadow(shape, g.shadowColor, spread, shadowAlpha)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .drawBehind {
            // Internal light: soft band along the top + small highlight in the top-left corner,
            // and a faint shade along the lower rim so the surface reads as having thickness.
            val bandH = size.height * 0.4f
            drawRect(
                Brush.verticalGradient(listOf(g.sheen, Color.Transparent), startY = 0f, endY = bandH),
                size = Size(size.width, bandH),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(g.sheen, Color.Transparent),
                    center = Offset(size.width * 0.14f, 0f),
                    radius = size.width * 0.5f,
                )
            )
            val shadeH = size.height * 0.35f
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, g.shade), startY = size.height - shadeH, endY = size.height),
                topLeft = Offset(0f, size.height - shadeH),
                size = Size(size.width, shadeH),
            )
        }
        .border(1.dp, Brush.linearGradient(listOf(g.borderTop, g.borderBottom), start = Offset.Zero, end = Offset.Infinite), shape)
}

/**
 * Soft shadow drawn only OUTSIDE [shape].
 *
 * Why not Modifier.shadow: it sets a platform elevation shadow. Android's renderer (HWUI) treats the
 * surface as opaque unless the layer's own alpha < 1; a translucent *fill* does not count. For an opaque
 * caster, Skia's shadow tessellator emits only a penumbra ring plus an inset convex umbra polygon and
 * leaves that polygon as an unshadowed hole. Under a see-through fill the hole shows as a sharp lighter
 * rectangle inside a grey rim.
 *
 * Here: a few widening strokes of the shape's outline, with the shape interior clipped out
 * (ClipOp.Difference), so nothing is ever painted under the glass. Draw modifiers are not clipped to
 * layout bounds; only an ancestor that clips would cut it (LazyColumn clips at its viewport, and the
 * shadow reach, spread + ~0.3*spread, stays inside the 16dp list padding / spacing).
 * Peak opacity is low, so a rendering fault would only mean "no shadow" or a faint tint.
 */
private fun Modifier.softShadow(shape: Shape, color: Color, spread: Dp, alpha: Float): Modifier =
    drawWithCache {
        val path = when (val o = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Generic -> o.path
            is Outline.Rectangle -> Path().apply { addRect(o.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(o.roundRect) }
        }
        val spreadPx = spread.toPx()
        val layers = 3
        val layerColor = color.copy(alpha = alpha / layers)
        onDrawBehind {
            clipPath(path, ClipOp.Difference) {
                translate(top = spreadPx * 0.3f) {
                    for (i in 1..layers) {
                        drawPath(
                            path,
                            layerColor,
                            style = Stroke(width = 2f * spreadPx * i / layers, join = StrokeJoin.Round),
                        )
                    }
                }
            }
        }
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
