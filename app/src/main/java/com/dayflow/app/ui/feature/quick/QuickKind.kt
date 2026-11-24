package com.dayflow.app.ui.feature.quick

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import com.dayflow.app.domain.model.ItemKind
import com.dayflow.app.ui.theme.DaySection

/** UI metadata for each quick-add target. Timer and Alarm entries are added in Phase 4 with real actions. */
enum class QuickKind(
    val item: ItemKind,
    val section: DaySection,
    val label: String,
    val hint: String,
    val icon: ImageVector,
) {
    HABIT(ItemKind.HABIT, DaySection.FOUNDATION, "Habit", "A habit you never want to skip", Icons.Rounded.LocalFireDepartment),
    IMPORTANT(ItemKind.IMPORTANT, DaySection.IMPORTANT, "Important task", "What deserves serious attention?", Icons.Rounded.Flag),
    NORMAL(ItemKind.NORMAL, DaySection.NORMAL, "Normal target", "A target for today", Icons.Rounded.TaskAlt),
    EXTRA(ItemKind.EXTRA, DaySection.EXTRA, "Extra target", "Something extra to attempt", Icons.Rounded.AutoAwesome),
    IDEA(ItemKind.IDEA, DaySection.IDEAS, "Idea", "Write it down before it slips away", Icons.Rounded.Lightbulb),
}
