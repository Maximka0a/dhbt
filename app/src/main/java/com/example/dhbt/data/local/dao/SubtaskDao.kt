package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY `order` ASC")
    fun getSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>>

    // Добавляем синхронный метод для получения подзадач
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    suspend fun getSubtasksForTaskSync(taskId: String): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE subtaskId = :subtaskId")
    suspend fun getSubtaskById(subtaskId: String): SubtaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: String)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted, completionDate = :completionDate WHERE subtaskId = :subtaskId")
    suspend fun updateSubtaskCompletion(subtaskId: String, isCompleted: Boolean, completionDate: Long?)

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId")
    suspend fun countSubtasksForTask(taskId: String): Int

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId AND isCompleted = 1")
    suspend fun countCompletedSubtasksForTask(taskId: String): Int
}