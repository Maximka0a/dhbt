package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    suspend fun getTagById(tagId: String): Tag?
    suspend fun addTag(tag: Tag): String
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(tagId: String)

    fun getTasksWithTag(tagId: String): Flow<List<Task>>
    fun getTagsForTask(taskId: String): Flow<List<Tag>>
}