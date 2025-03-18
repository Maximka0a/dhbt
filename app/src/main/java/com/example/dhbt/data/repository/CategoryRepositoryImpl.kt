package com.example.dhbt.data.repository

import com.example.dhbt.data.local.dao.CategoryDao
import com.example.dhbt.data.mapper.CategoryMapper
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val categoryMapper: CategoryMapper
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { categoryMapper.mapFromEntity(it) }
        }
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.value).map { entities ->
            entities.map { categoryMapper.mapFromEntity(it) }
        }
    }

    override suspend fun getCategoryById(categoryId: String): Category? {
        val entity = categoryDao.getCategoryById(categoryId) ?: return null
        return categoryMapper.mapFromEntity(entity)
    }

    override suspend fun addCategory(category: Category): String {
        val entity = categoryMapper.mapToEntity(category)
        categoryDao.insertCategory(entity)
        return category.id
    }

    override suspend fun updateCategory(category: Category) {
        val entity = categoryMapper.mapToEntity(category)
        categoryDao.updateCategory(entity)
    }

    override suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteCategoryById(categoryId)
    }

    override suspend fun getTaskCountInCategory(categoryId: String): Int {
        return categoryDao.countTasksInCategory(categoryId)
    }

    override suspend fun getHabitCountInCategory(categoryId: String): Int {
        return categoryDao.countHabitsInCategory(categoryId)
    }
}