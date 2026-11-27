package com.dayflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfShapes

/** Frosted-glass surface: soft shadow, translucent gradient fill, gradient hairline border. */
@Composable
fun Modifier.glass(
    shape: Shape = DfShapes.glass,
    tint: Color = Color.Unspecified,
    raised: Boolean = false,
): Modifier {
    val g = Df.glass
    val top = if (tint == Color.Unspecified) g.fillTop else tint.copy(alpha = g.tintAlpha).compositeOver(g.fillTop)
    val bottom = if (tint == Color.Unspecified) g.fillBottom else tint.copy(alpha = g.tintAlpha * 0.6f).compositeOver(g.fillBottom)
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
