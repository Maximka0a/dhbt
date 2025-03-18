package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE tagId = :tagId")
    suspend fun getTagById(tagId: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE tagId = :tagId")
    suspend fun deleteTagById(tagId: String)

    @Transaction
    @Query("SELECT t.* FROM tags t INNER JOIN task_tag_cross_refs ref ON t.tagId = ref.tagId WHERE ref.taskId = :taskId")
    fun getTagsForTask(taskId: String): Flow<List<TagEntity>>
}