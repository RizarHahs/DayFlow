package com.dayflow.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalSectionPalette = staticCompositionLocalOf { sectionPalette(DaySection.FOUNDATION, true) }

/** Short accessor: `Df.colors`, `Df.section`. */
object Df {
    val colors: DfColors
        @Composable @ReadOnlyComposable get() = LocalDfColors.current
    val section: SectionPalette
        @Composable @ReadOnlyComposable get() = LocalSectionPalette.current
    val glass: DfGlass
        @Composable @ReadOnlyComposable get() = LocalGlass.current
}

/** Re-skins every shared component (check, ring, chip, empty state) for one section. */
@Composable
fun SectionTheme(section: DaySection, content: @Composable () -> Unit) {
    val dark = LocalDfColors.current.isDark
    CompositionLocalProvider(
        LocalSectionPalette provides sectionPalette(section, dark),
        content = content,
    )
}

@Composable
fun DayflowTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val c = if (darkTheme) DarkDfColors else LightDfColors
    val brand = if (darkTheme) Color(0xFF34D399) else Color(0xFF0E9F6E)
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = brand, onPrimary = Color(0xFF03261C),
            background = c.background, onBackground = c.textPrimary,
            surface = c.surface, onSurface = c.textPrimary,
            surfaceVariant = c.raised, onSurfaceVariant = c.textSecondary,
            outline = c.hairline,
        )
    } else {
        lightColorScheme(
            primary = brand, onPrimary = Color.White,
            background = c.background, onBackground = c.textPrimary,
            surface = c.surface, onSurface = c.textPrimary,
            surfaceVariant = c.raised, onSurfaceVariant = c.textSecondary,
            outline = c.hairline,
        )
    }
    CompositionLocalProvider(
        LocalDfColors provides c,
        LocalGlass provides (if (darkTheme) DarkGlass else LightGlass),
        LocalSectionPalette provides sectionPalette(DaySection.FOUNDATION, darkTheme),
    ) {
        MaterialTheme(colorScheme = scheme, typography = M3Type, shapes = M3Shapes, content = content)
    }
}
