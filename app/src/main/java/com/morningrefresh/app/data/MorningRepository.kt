package com.morningrefresh.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class MorningRepository(private val database: AppDatabase) {
    private val alarms = database.alarmDao()
    private val tasks = database.taskDao()
    private val checkIns = database.checkInDao()
    private val goals = database.weeklyGoalDao()

    fun observeToday(date: LocalDate = LocalDate.now()): Flow<TodaySnapshot> {
        val day = date.toEpochDay()
        return combine(
            tasks.observeForDate(day),
            alarms.observeAll(),
            checkIns.observeForDate(day),
            goals.observeAll(),
        ) { todayTasks, allAlarms, checkIn, allGoals ->
            TodaySnapshot(todayTasks, allAlarms, checkIn, allGoals.filter { it.weekStartEpochDay <= day })
        }
    }

    suspend fun seedIfNeeded(date: LocalDate = LocalDate.now()) {
        if (tasks.forDate(date.toEpochDay()).isNotEmpty()) return
        val day = date.toEpochDay()
        tasks.insertAll(
            listOf(
                TaskEntity(
                    title = "Morning class",
                    dateEpochDay = day,
                    startMinutes = 9 * 60,
                    durationMinutes = 90,
                    priority = 3,
                    kind = TaskKind.FIXED.name,
                ),
                TaskEntity(
                    title = "Workout",
                    dateEpochDay = day,
                    startMinutes = 11 * 60,
                    durationMinutes = 45,
                    priority = 2,
                    kind = TaskKind.FLEXIBLE.name,
                ),
                TaskEntity(
                    title = "Project practice",
                    dateEpochDay = day,
                    startMinutes = 13 * 60,
                    durationMinutes = 60,
                    priority = 2,
                    kind = TaskKind.FLEXIBLE.name,
                ),
                TaskEntity(
                    title = "Read 20 pages",
                    dateEpochDay = day,
                    startMinutes = 17 * 60,
                    durationMinutes = 30,
                    priority = 1,
                    kind = TaskKind.FLEXIBLE.name,
                ),
            ),
        )
        if (goals.observeAllSnapshot().isEmpty()) {
            goals.insert(
                WeeklyGoalEntity(
                    title = "Workout three times",
                    targetCount = 3,
                    weekStartEpochDay = weekStart(date).toEpochDay(),
                ),
            )
        }
    }

    suspend fun saveAlarm(alarm: AlarmEntity): Long = alarms.insert(alarm)
    suspend fun updateAlarm(alarm: AlarmEntity) = alarms.update(alarm)
    suspend fun deleteAlarm(alarm: AlarmEntity) = alarms.delete(alarm)
    suspend fun saveCheckIn(checkIn: CheckInEntity) = checkIns.save(checkIn)
    suspend fun insertTask(task: TaskEntity): Long = tasks.insert(task)
    suspend fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        tasks.setCompleted(task.id, completed)
        task.goalId?.let { goalId ->
            val count = tasks.completedForGoal(goalId)
            goals.findById(goalId)?.let { goals.update(it.copy(completedCount = count)) }
        }
    }

    suspend fun replanFlexibleTasks(date: LocalDate, delayMinutes: Int): Int {
        val future = tasks.forDate(date.toEpochDay())
            .filter { !it.completed && it.kind == TaskKind.FLEXIBLE.name && it.startMinutes >= 10 * 60 }
        future.forEach { tasks.move(it.id, (it.startMinutes + delayMinutes).coerceAtMost(22 * 60)) }
        return future.size
    }

    suspend fun addGoal(title: String, targetCount: Int, date: LocalDate) {
        goals.insert(
            WeeklyGoalEntity(
                title = title,
                targetCount = targetCount,
                weekStartEpochDay = weekStart(date).toEpochDay(),
            ),
        )
    }

    suspend fun saveGoalTask(goal: WeeklyGoalEntity, date: LocalDate): Long = tasks.insert(
        TaskEntity(
            title = goal.title,
            dateEpochDay = date.toEpochDay(),
            startMinutes = 18 * 60,
            durationMinutes = 45,
            priority = 2,
            kind = TaskKind.FLEXIBLE.name,
            goalId = goal.id,
        ),
    )

    suspend fun enabledAlarms(): List<AlarmEntity> = alarms.enabled()

    suspend fun completeAndReplan(task: TaskEntity, date: LocalDate): Int {
        setTaskCompleted(task, true)
        val now = LocalDateTime.now()
        val plannedEnd = task.startMinutes + task.durationMinutes
        val nowMinutes = now.hour * 60 + now.minute
        val delay = (nowMinutes - plannedEnd).coerceIn(0, 90)
        return if (delay > 0) replanFlexibleTasks(date, delay) else 0
    }

    private fun weekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)
}

private suspend fun WeeklyGoalDao.observeAllSnapshot(): List<WeeklyGoalEntity> = observeAll().first()
private suspend fun WeeklyGoalDao.findById(id: Long): WeeklyGoalEntity? = observeAllSnapshot().firstOrNull { it.id == id }
