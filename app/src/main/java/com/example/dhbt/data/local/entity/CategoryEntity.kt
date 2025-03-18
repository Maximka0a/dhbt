package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val categoryId: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String? = null,
    val iconEmoji: String? = null,
    val type: Int, // 0-задача, 1-привычка, 2-оба
    val order: Int = 0 // Порядок отображения
)