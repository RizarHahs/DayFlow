package com.dayflow.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glass tokens. Translucent layered fills + gradient hairline border (bright top edge = inner highlight).
 * No real backdrop blur: Compose cannot blur what is behind a surface without an extra library,
 * and per-card blur is costly. Depth comes from fills, borders and soft shadow instead.
 */
@Immutable
data class DfGlass(
    val fillTop: Color,
    val fillBottom: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val shadowColor: Color,
    val shadow: Dp,
    val shadowRaised: Dp,
    val tintAlpha: Float,
)

val DarkGlass = DfGlass(
    fillTop = Color(0x1FFFFFFF),
    fillBottom = Color(0x0DFFFFFF),
    borderTop = Color(0x3DFFFFFF),
    borderBottom = Color(0x0FFFFFFF),
    shadowColor = Color(0xFF000000),
    shadow = 6.dp,
    shadowRaised = 14.dp,
    tintAlpha = 0.12f,
)

val LightGlass = DfGlass(
    fillTop = Color(0xB8FFFFFF),
    fillBottom = Color(0x8CFFFFFF),
    borderTop = Color(0xE6FFFFFF),
    borderBottom = Color(0x14000000),
    shadowColor = Color(0xFF1A2230),
    shadow = 4.dp,
    shadowRaised = 10.dp,
    tintAlpha = 0.10f,
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }
