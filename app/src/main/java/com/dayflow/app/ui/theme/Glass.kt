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
    /** Outside-only soft shadow: spread, and peak opacity. Kept low so any rendering fault is benign. */
    val shadow: Dp,
    val shadowRaised: Dp,
    val shadowAlpha: Float,
    /** Faint internal reflection along the top, and a faint lower-rim shade for depth. */
    val sheen: Color,
    val shade: Color,
    val tintAlpha: Float,
    /** Denser fills for bars and sheets, where scrolling content passes underneath (no backdrop blur). */
    val chromeTop: Color,
    val chromeBottom: Color,
)

val DarkGlass = DfGlass(
    fillTop = Color(0x1CE8EEFF),
    fillBottom = Color(0x0BE8EEFF),
    borderTop = Color(0x33FFFFFF),
    borderBottom = Color(0x08FFFFFF),
    shadowColor = Color(0xFF000000),
    shadow = 8.dp,
    shadowRaised = 14.dp,
    shadowAlpha = 0.30f,
    sheen = Color(0x12FFFFFF),
    shade = Color(0x14000000),
    tintAlpha = 0.12f,
    chromeTop = Color(0xF2202329),
    chromeBottom = Color(0xF21A1D22),
)

val LightGlass = DfGlass(
    fillTop = Color(0x8CF6F8FF),
    fillBottom = Color(0x57E9EFFA),
    borderTop = Color(0xB3FFFFFF),
    borderBottom = Color(0x1F6B7C9C),
    shadowColor = Color(0xFF2A3A5C),
    shadow = 8.dp,
    shadowRaised = 14.dp,
    shadowAlpha = 0.12f,
    sheen = Color(0x40FFFFFF),
    shade = Color(0x0F5A6B8C),
    tintAlpha = 0.10f,
    chromeTop = Color(0xF2FFFFFF),
    chromeBottom = Color(0xE6F7F8FB),
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }
