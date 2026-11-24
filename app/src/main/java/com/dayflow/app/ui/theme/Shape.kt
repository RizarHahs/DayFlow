package com.dayflow.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object DfShapes {
    val small = RoundedCornerShape(10.dp)
    val card = RoundedCornerShape(14.dp)
    val panel = RoundedCornerShape(22.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

val M3Shapes = Shapes(
    small = DfShapes.small,
    medium = DfShapes.card,
    large = DfShapes.panel,
)
