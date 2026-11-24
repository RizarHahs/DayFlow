package com.dayflow.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

/** The five bottom-bar destinations (see Phase 1 navigation map). */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Rounded.Home),
    ROADMAP("roadmap", "Roadmap", Icons.Rounded.Schedule),
    TASKS("tasks", "Tasks", Icons.Rounded.Checklist),
    IDEAS("ideas", "Ideas", Icons.Rounded.Lightbulb),
    INSIGHTS("insights", "Insights", Icons.Rounded.Insights);

    companion object {
        fun fromRoute(route: String?) = entries.firstOrNull { it.route == route } ?: HOME
    }
}
