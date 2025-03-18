package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.TaskRecurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskRecurrenceDao {
    @Query("SELECT * FROM task_recurrences")
    fun getAllTaskRecurrences(): Flow<List<TaskRecurrenceEntity>>

    @Query("SELECT * FROM task_recurrences WHERE taskId = :taskId")
    suspend fun getRecurrenceForTask(taskId: String): TaskRecurrenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskRecurrence(taskRecurrence: TaskRecurrenceEntity)

    @Update
    suspend fun updateTaskRecurrence(taskRecurrence: TaskRecurrenceEntity)

    @Delete
    suspend fun deleteTaskRecurrence(taskRecurrence: TaskRecurrenceEntity)

    @Query("DELETE FROM task_recurrences WHERE taskId = :taskId")
    suspend fun deleteRecurrenceForTask(taskId: String)
}