package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY `order` ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type OR type = 2 ORDER BY `order` ASC")
    fun getCategoriesByType(type: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE categoryId = :categoryId")
    suspend fun deleteCategoryById(categoryId: String)

    @Query("SELECT COUNT(*) FROM tasks WHERE categoryId = :categoryId")
    suspend fun countTasksInCategory(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM habits WHERE categoryId = :categoryId")
    suspend fun countHabitsInCategory(categoryId: String): Int
}