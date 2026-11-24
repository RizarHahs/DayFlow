package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.CategoryProgress
import com.dayflow.app.domain.model.ScoreWeights

/**
 * Phase 1 scoring rules:
 *  - every category keeps its own progress
 *  - weighted score re-normalises over categories that actually have items today
 *  - Extra never lowers the score
 *  - Day Complete = every non-empty core category is fully done
 */
object DayScoring {

    fun weighted(
        foundation: CategoryProgress,
        important: CategoryProgress,
        normal: CategoryProgress,
        w: ScoreWeights = ScoreWeights(),
    ): Float {
        val parts = listOf(foundation to w.foundation, important to w.important, normal to w.normal)
            .filter { !it.first.isEmpty }
        val weightSum = parts.sumOf { it.second.toDouble() }
        if (weightSum <= 0.0) return 0f
        val acc = parts.sumOf { (it.first.fraction * it.second).toDouble() }
        return (acc / weightSum).toFloat()
    }

    fun dayComplete(vararg core: CategoryProgress): Boolean {
        val active = core.filter { !it.isEmpty }
        return active.isNotEmpty() && active.all { it.done == it.total }
    }
}
