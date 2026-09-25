package com.morningrefresh.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskKind { FIXED, FLEXIBLE }

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "Morning alarm",
    val hour: Int,
    val minute: Int,
    val repeatMask: Int = 127,
    val enabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val challengeLevel: Int = 1,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateEpochDay: Long,
    val startMinutes: Int,
    val durationMinutes: Int,
    val priority: Int = 2,
    val kind: String = TaskKind.FLEXIBLE.name,
    val completed: Boolean = false,
    val goalId: Long? = null,
    val note: String = "",
)

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey val dateEpochDay: Long,
    val sleepHours: Int,
    val energy: Int,
    val mood: Int,
    val stress: Int,
    val readiness: Int,
)

@Entity(tableName = "weekly_goals")
data class WeeklyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetCount: Int,
    val completedCount: Int = 0,
    val weekStartEpochDay: Long,
)

data class TodaySnapshot(
    val tasks: List<TaskEntity> = emptyList(),
    val alarms: List<AlarmEntity> = emptyList(),
    val checkIn: CheckInEntity? = null,
    val goals: List<WeeklyGoalEntity> = emptyList(),
)
