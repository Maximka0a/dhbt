package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDate ASC")
    fun getTasksByStatus(status: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE taskId = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId")
    fun getTasksByCategory(categoryId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :startDate AND :endDate")
    fun getTasksByDateRange(startDate: Long, endDate: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priority = :priority")
    fun getTasksByPriority(priority: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE eisenhowerQuadrant = :quadrant")
    fun getTasksByEisenhowerQuadrant(quadrant: Int): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("UPDATE tasks SET status = :status WHERE taskId = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: Int)

    @Query("UPDATE tasks SET completionDate = :completionDate WHERE taskId = :taskId")
    suspend fun updateTaskCompletion(taskId: String, completionDate: Long)
}