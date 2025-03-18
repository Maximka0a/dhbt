package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val tagId: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String? = null
)