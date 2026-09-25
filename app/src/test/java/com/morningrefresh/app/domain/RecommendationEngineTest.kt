package com.morningrefresh.app.domain

import com.morningrefresh.app.data.CheckInEntity
import com.morningrefresh.app.data.TaskEntity
import com.morningrefresh.app.data.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    @Test
    fun missingCheckInAsksForCheckIn() {
        val recommendation = RecommendationEngine.build(null, emptyList())
        assertEquals(0, recommendation.score)
        assertTrue(recommendation.title.contains("check-in", ignoreCase = true))
    }

    @Test
    fun lowReadinessWithFlexibleTasksSuggestsGentleMorning() {
        val checkIn = CheckInEntity(1, 5, 2, 3, 8, 30)
        val tasks = listOf(
            TaskEntity(title = "Workout", dateEpochDay = 1, startMinutes = 600, durationMinutes = 45, kind = TaskKind.FLEXIBLE.name),
        )
        val recommendation = RecommendationEngine.build(checkIn, tasks)
        assertEquals("Keep the morning gentle", recommendation.title)
    }

    @Test
    fun heavyWorkloadUsesFocusBlockRecommendation() {
        val checkIn = CheckInEntity(1, 8, 9, 8, 2, 80)
        val tasks = (1..4).map { index ->
            TaskEntity(title = "Task $index", dateEpochDay = 1, startMinutes = 600 + index * 30, durationMinutes = 30)
        }
        val recommendation = RecommendationEngine.build(checkIn, tasks)
        assertEquals("Use your best focus block", recommendation.title)
    }
}
