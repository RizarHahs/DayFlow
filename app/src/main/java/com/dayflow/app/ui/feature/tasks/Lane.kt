package com.dayflow.app.ui.feature.tasks

import com.dayflow.app.domain.model.TaskKind
import com.dayflow.app.ui.theme.DaySection

enum class Lane(val label: String, val section: DaySection) {
    FOUNDATION("Habits", DaySection.FOUNDATION),
    IMPORTANT("Important", DaySection.IMPORTANT),
    NORMAL("Targets", DaySection.NORMAL),
    EXTRA("Extra", DaySection.EXTRA);

    val taskKind: TaskKind? get() = when (this) {
        IMPORTANT -> TaskKind.IMPORTANT
        NORMAL -> TaskKind.NORMAL
        EXTRA -> TaskKind.EXTRA
        FOUNDATION -> null
    }
}
