package com.dayflow.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Neutral surface system shared by every screen. */
@Immutable
data class DfColors(
    val background: Color,
    val surface: Color,
    val raised: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val isDark: Boolean,
)

val DarkDfColors = DfColors(
    background = Color(0xFF0E0F12),
    surface = Color(0xFF16181D),
    raised = Color(0xFF1D2026),
    hairline = Color(0x1FFFFFFF),
    textPrimary = Color(0xFFECEEF2),
    textSecondary = Color(0xFF9AA0AC),
    textTertiary = Color(0xFF6B7280),
    isDark = true,
)

val LightDfColors = DfColors(
    background = Color(0xFFF5F4F0),
    surface = Color(0xFFFFFFFF),
    raised = Color(0xFFEDEBE5),
    hairline = Color(0x1F000000),
    textPrimary = Color(0xFF16181D),
    textSecondary = Color(0xFF5B616E),
    textTertiary = Color(0xFF8A909C),
    isDark = false,
)

val LocalDfColors = staticCompositionLocalOf { DarkDfColors }

/** The five functional environments of the app. */
enum class DaySection { FOUNDATION, IMPORTANT, NORMAL, EXTRA, IDEAS }

@Immutable
data class SectionPalette(
    val accent: Color,
    val deep: Color,
    val soft: Color,
    val onAccent: Color,
)

fun sectionPalette(section: DaySection, dark: Boolean): SectionPalette = when (section) {
    DaySection.FOUNDATION -> if (dark)
        SectionPalette(Color(0xFF34D399), Color(0xFF064E3B), Color(0x2634D399), Color(0xFF03261C))
    else
        SectionPalette(Color(0xFF0E9F6E), Color(0xFFD1FAE5), Color(0x1F0E9F6E), Color.White)

    DaySection.IMPORTANT -> if (dark)
        SectionPalette(Color(0xFFF59E0B), Color(0xFF4A2A06), Color(0x26F59E0B), Color(0xFF2B1700))
    else
        SectionPalette(Color(0xFFD97706), Color(0xFFFEF3C7), Color(0x1FD97706), Color.White)

    DaySection.NORMAL -> if (dark)
        SectionPalette(Color(0xFF38BDF8), Color(0xFF0C3A5C), Color(0x2638BDF8), Color(0xFF04202F))
    else
        SectionPalette(Color(0xFF0284C7), Color(0xFFE0F2FE), Color(0x1F0284C7), Color.White)

    DaySection.EXTRA -> if (dark)
        SectionPalette(Color(0xFFA78BFA), Color(0xFF2E1B5E), Color(0x26A78BFA), Color(0xFF1B0F3D))
    else
        SectionPalette(Color(0xFF7C3AED), Color(0xFFEDE9FE), Color(0x1F7C3AED), Color.White)

    DaySection.IDEAS -> if (dark)
        SectionPalette(Color(0xFFCFC6F2), Color(0xFF2A2638), Color(0x26CFC6F2), Color(0xFF241F36))
    else
        SectionPalette(Color(0xFF6D5BD0), Color(0xFFEFE8D8), Color(0x1F6D5BD0), Color.White)
}

object PriorityColors {
    val Critical = Color(0xFFEA580C)
    val High = Color(0xFFF59E0B)
    val Medium = Color(0xFFC9A94B)
}

/** "Paper" look used only by Ideas of the Day. */
object Paper {
    val Sheet = Color(0xFFEFE8D8)
    val Ink = Color(0xFF2B2620)
    val InkSoft = Color(0xFF6E665A)
    val Rule = Color(0x1F2B2620)
    val Margin = Color(0x55C97B7B)
    val Lavender = Color(0xFFCFC6F2)
    val Tape = Color(0xB3B9A9F0)
}

val ExtraGradient = listOf(Color(0xFFA78BFA), Color(0xFFE879F9))
val BrandRing = listOf(
    Color(0xFF34D399), Color(0xFFF59E0B), Color(0xFF38BDF8), Color(0xFFA78BFA), Color(0xFF34D399),
)
