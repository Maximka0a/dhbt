package com.example.dhbt.data.repository

import com.example.dhbt.data.local.dao.TagDao
import com.example.dhbt.data.local.dao.TaskTagDao
import com.example.dhbt.data.mapper.TagMapper
import com.example.dhbt.data.mapper.TaskMapper
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val taskTagDao: TaskTagDao,
    private val tagMapper: TagMapper,
    private val taskMapper: TaskMapper
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { tagEntities ->
            tagEntities.map { tagEntity ->
                tagMapper.mapFromEntity(tagEntity)
            }
        }
    }

    override suspend fun getTagById(tagId: String): Tag? {
        val entity = tagDao.getTagById(tagId) ?: return null
        return tagMapper.mapFromEntity(entity)
    }

    override suspend fun addTag(tag: Tag): String {
        val entity = tagMapper.mapToEntity(tag)
        tagDao.insertTag(entity)
        return tag.id
    }

    override suspend fun updateTag(tag: Tag) {
        val entity = tagMapper.mapToEntity(tag)
        tagDao.updateTag(entity)
    }

    override suspend fun deleteTag(tagId: String) {
        tagDao.deleteTagById(tagId)
    }

    override fun getTasksWithTag(tagId: String): Flow<List<Task>> {
        return taskTagDao.getTasksWithTag(tagId).map { taskEntities ->
            taskEntities.map { taskMapper.mapFromEntity(it) }
        }
    }

    override fun getTagsForTask(taskId: String): Flow<List<Tag>> {
        return tagDao.getTagsForTask(taskId).map { entities ->
            entities.map { tagMapper.mapFromEntity(it) }
        }
    }
}