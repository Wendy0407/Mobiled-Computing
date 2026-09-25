package com.morningrefresh.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.morningrefresh.app.alarm.AlarmScheduler
import com.morningrefresh.app.data.AlarmEntity
import com.morningrefresh.app.data.AppSettings
import com.morningrefresh.app.data.CheckInEntity
import com.morningrefresh.app.data.MorningRepository
import com.morningrefresh.app.data.TaskEntity
import com.morningrefresh.app.data.TaskKind
import com.morningrefresh.app.data.TodaySnapshot
import com.morningrefresh.app.data.WeeklyGoalEntity
import com.morningrefresh.app.data.SettingsStore
import com.morningrefresh.app.domain.DailyRecommendation
import com.morningrefresh.app.domain.RecommendationEngine
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class MorningUiState(
    val snapshot: TodaySnapshot = TodaySnapshot(),
    val settings: AppSettings = AppSettings(),
    val recommendation: DailyRecommendation = DailyRecommendation(
        "Start with a quick check-in",
        "Tell Morning Refresh how you feel so it can adjust today's flexible tasks.",
        0,
    ),
    val message: String? = null,
)

class MorningViewModel(
    private val repository: MorningRepository,
    private val settingsStore: SettingsStore,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MorningUiState())
    val uiState: StateFlow<MorningUiState> = _uiState.asStateFlow()
    private val today = LocalDate.now()

    init {
        viewModelScope.launch { repository.seedIfNeeded(today) }
        viewModelScope.launch {
            combine(repository.observeToday(today), settingsStore.settings) { snapshot, settings ->
                MorningUiState(
                    snapshot = snapshot,
                    settings = settings,
                    recommendation = RecommendationEngine.build(snapshot.checkIn, snapshot.tasks),
                    message = _uiState.value.message,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun saveAlarm(hour: Int, minute: Int, enabled: Boolean = true) {
        viewModelScope.launch {
            val snooze = _uiState.value.settings.snoozeMinutes
            val id = repository.saveAlarm(
                AlarmEntity(
                    label = "Morning alarm",
                    hour = hour,
                    minute = minute,
                    enabled = enabled,
                    snoozeMinutes = snooze,
                ),
            )
            alarmScheduler.schedule(
                AlarmEntity(
                    id = id,
                    label = "Morning alarm",
                    hour = hour,
                    minute = minute,
                    enabled = enabled,
                    snoozeMinutes = snooze,
                ),
            )
            showMessage("Alarm saved for %02d:%02d".format(hour, minute))
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(enabled = !alarm.enabled)
            repository.updateAlarm(updated)
            if (updated.enabled) alarmScheduler.schedule(updated) else alarmScheduler.cancel(updated)
            showMessage(if (updated.enabled) "Alarm enabled" else "Alarm paused")
        }
    }

    fun addTask(title: String, startMinutes: Int, durationMinutes: Int, fixed: Boolean) {
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = title.ifBlank { "New task" },
                    dateEpochDay = today.toEpochDay(),
                    startMinutes = startMinutes.coerceIn(0, 23 * 60 + 59),
                    durationMinutes = durationMinutes.coerceIn(15, 240),
                    kind = if (fixed) TaskKind.FIXED.name else TaskKind.FLEXIBLE.name,
                ),
            )
            showMessage("Task added to today's plan")
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            val moved = if (task.completed) {
                repository.setTaskCompleted(task, false)
                0
            } else {
                repository.completeAndReplan(task, today)
            }
            if (moved > 0) showMessage("$moved flexible task(s) moved to protect your plan")
        }
    }

    fun simulateDelay() {
        viewModelScope.launch {
            val moved = repository.replanFlexibleTasks(today, 30)
            showMessage(if (moved == 0) "No flexible tasks are available to move" else "$moved flexible task(s) moved by 30 minutes")
        }
    }

    fun saveCheckIn(sleep: Int, energy: Int, mood: Int, stress: Int) {
        viewModelScope.launch {
            val safeSleep = sleep.coerceIn(0, 10)
            val safeEnergy = energy.coerceIn(0, 10)
            val safeMood = mood.coerceIn(0, 10)
            val safeStress = stress.coerceIn(0, 10)
            val readiness = (safeSleep * 10 + safeEnergy * 10 + safeMood * 10 + (10 - safeStress) * 10) / 4
            repository.saveCheckIn(
                CheckInEntity(
                    dateEpochDay = today.toEpochDay(),
                    sleepHours = sleep,
                    energy = energy,
                    mood = mood,
                    stress = stress,
                    readiness = readiness.coerceIn(0, 100),
                ),
            )
            showMessage("Check-in saved and today's plan refreshed")
        }
    }

    fun addGoal(title: String, target: Int) {
        viewModelScope.launch {
            repository.addGoal(title.ifBlank { "Weekly goal" }, target.coerceIn(1, 7), today)
            showMessage("Weekly goal added")
        }
    }

    fun scheduleGoalTask(goal: WeeklyGoalEntity) {
        viewModelScope.launch {
            repository.saveGoalTask(goal, today)
            showMessage("One session added to today's flexible plan")
        }
    }

    fun changeSnooze(delta: Int) {
        viewModelScope.launch {
            settingsStore.setSnoozeMinutes(_uiState.value.settings.snoozeMinutes + delta)
        }
    }

    private fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    class Factory(
        private val repository: MorningRepository,
        private val settingsStore: SettingsStore,
        private val alarmScheduler: AlarmScheduler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MorningViewModel(
            repository,
            settingsStore,
            alarmScheduler,
        ) as T
    }
}
