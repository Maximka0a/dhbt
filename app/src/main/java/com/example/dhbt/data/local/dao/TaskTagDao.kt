package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.TaskTagCrossRef
import kotlinx.coroutines.flow.Flow
import com.example.dhbt.data.local.entity.TaskEntity

@Dao
interface TaskTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTagCrossRef(crossRef: TaskTagCrossRef)

    @Delete
    suspend fun deleteTaskTagCrossRef(crossRef: TaskTagCrossRef)

    @Query("DELETE FROM task_tag_cross_refs WHERE taskId = :taskId")
    suspend fun deleteAllTagsForTask(taskId: String)

    @Query("DELETE FROM task_tag_cross_refs WHERE tagId = :tagId")
    suspend fun deleteAllTasksForTag(tagId: String)

    @Transaction
    @Query("SELECT tasks.* FROM tasks INNER JOIN task_tag_cross_refs ON tasks.taskId = task_tag_cross_refs.taskId WHERE task_tag_cross_refs.tagId = :tagId")
    fun getTasksWithTag(tagId: String): Flow<List<TaskEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM task_tag_cross_refs WHERE taskId = :taskId AND tagId = :tagId)")
    suspend fun isTaskTagged(taskId: String, tagId: String): Boolean

    @Query("SELECT * FROM task_tag_cross_refs WHERE taskId = :taskId")
    suspend fun getTaskTagCrossRefsForTask(taskId: String): List<TaskTagCrossRef>
}