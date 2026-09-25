package com.morningrefresh.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY hour, minute")
    suspend fun enabled(): List<AlarmEntity>

    @Insert
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE dateEpochDay = :date ORDER BY startMinutes, priority DESC")
    fun observeForDate(date: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dateEpochDay = :date ORDER BY startMinutes, priority DESC")
    suspend fun forDate(date: Long): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET completed = :completed WHERE id = :taskId")
    suspend fun setCompleted(taskId: Long, completed: Boolean)

    @Query("UPDATE tasks SET startMinutes = :startMinutes WHERE id = :taskId")
    suspend fun move(taskId: Long, startMinutes: Int)

    @Query("SELECT COUNT(*) FROM tasks WHERE goalId = :goalId AND completed = 1")
    suspend fun completedForGoal(goalId: Long): Int
}

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins WHERE dateEpochDay = :date LIMIT 1")
    fun observeForDate(date: Long): Flow<CheckInEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(checkIn: CheckInEntity)
}

@Dao
interface WeeklyGoalDao {
    @Query("SELECT * FROM weekly_goals ORDER BY weekStartEpochDay DESC, id")
    fun observeAll(): Flow<List<WeeklyGoalEntity>>

    @Insert
    suspend fun insert(goal: WeeklyGoalEntity): Long

    @Update
    suspend fun update(goal: WeeklyGoalEntity)
}
