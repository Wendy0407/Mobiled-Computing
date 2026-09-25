package com.morningrefresh.app.domain

import com.morningrefresh.app.data.CheckInEntity
import com.morningrefresh.app.data.TaskEntity
import com.morningrefresh.app.data.TaskKind

data class DailyRecommendation(
    val title: String,
    val body: String,
    val score: Int,
)

object RecommendationEngine {
    fun build(checkIn: CheckInEntity?, tasks: List<TaskEntity>): DailyRecommendation {
        if (checkIn == null) {
            return DailyRecommendation(
                title = "Start with a quick check-in",
                body = "Tell Morning Refresh how you feel so it can adjust today's flexible tasks.",
                score = 0,
            )
        }

        val unfinished = tasks.count { !it.completed }
        val flexible = tasks.count { !it.completed && it.kind == TaskKind.FLEXIBLE.name }
        val heavyLoad = unfinished >= 4
        return when {
            checkIn.readiness <= 40 && flexible > 0 -> DailyRecommendation(
                title = "Keep the morning gentle",
                body = "Try water, light movement and breakfast first. Delay or shorten low-priority flexible tasks.",
                score = checkIn.readiness,
            )
            checkIn.readiness <= 40 -> DailyRecommendation(
                title = "Protect the fixed plan",
                body = "Keep fixed commitments and leave a recovery break after them.",
                score = checkIn.readiness,
            )
            heavyLoad -> DailyRecommendation(
                title = "Use your best focus block",
                body = "Start with the most important task, then let the flexible tasks fill the remaining time.",
                score = checkIn.readiness,
            )
            else -> DailyRecommendation(
                title = "A steady morning is enough",
                body = "Keep the plan realistic and finish one task before starting another.",
                score = checkIn.readiness,
            )
        }
    }
}
