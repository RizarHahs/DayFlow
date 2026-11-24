package com.dayflow.app.ui.nav

import com.dayflow.app.domain.model.TaskKind

/** Full-screen editor routes (no bottom bar / FAB). Id 0 = create. */
object Routes {
    const val HABIT = "habit/{id}"
    const val TASK = "task/{id}/{kind}"
    const val IDEA = "idea/{id}"
    const val SLOT = "slot/{id}"
    const val ALARM = "alarm/{id}"
    const val REVIEW = "review/{day}"
    const val MESSAGES = "messages"
    const val MESSAGE = "message/{id}"
    const val ALARMS = "alarms"
    const val TIMER = "timer"
    const val SETTINGS = "settings"

    fun habit(id: Long = 0) = "habit/$id"
    fun task(id: Long = 0, kind: TaskKind) = "task/$id/${kind.name}"
    fun idea(id: Long = 0) = "idea/$id"
    fun slot(id: Long = 0) = "slot/$id"
    fun alarm(id: Long = 0) = "alarm/$id"
    fun review(day: Int) = "review/$day"
    fun message(id: Long = 0) = "message/$id"
}
