package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>

    suspend fun getCategoryById(categoryId: String): Category?
    suspend fun addCategory(category: Category): String
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(categoryId: String)

    suspend fun getTaskCountInCategory(categoryId: String): Int
    suspend fun getHabitCountInCategory(categoryId: String): Int
}