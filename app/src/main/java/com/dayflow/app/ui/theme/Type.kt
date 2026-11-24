package com.dayflow.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dayflow.app.R

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight, style: FontStyle = FontStyle.Normal) = Font(
    resId = resId,
    weight = weight,
    style = style,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/** UI face. */
val Sora = FontFamily(weights.map { variable(R.font.sora_variable, it) })

/** Clock / timers / counts. */
val JetBrainsMono = FontFamily(weights.map { variable(R.font.jetbrains_mono_variable, it) })

/** Ideas notebook. */
val Newsreader = FontFamily(weights.map { variable(R.font.newsreader_variable, it) })

object DfType {
    val display = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp)
    val numeral = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1).sp)
    val title = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)
    val titleSmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val bodyMedium = body.copy(fontWeight = FontWeight.Medium)
    val bodySmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val label = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp)
    val mono = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val monoLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp)
    val serifTitle = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp)
    val serifBody = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp)
    val serifSmall = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp)
}

val M3Type = Typography(
    displayLarge = DfType.numeral,
    headlineMedium = DfType.display,
    titleLarge = DfType.title,
    titleMedium = DfType.titleSmall,
    bodyLarge = DfType.body,
    bodyMedium = DfType.bodySmall,
    labelLarge = DfType.bodyMedium,
    labelSmall = DfType.label,
)
