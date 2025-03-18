package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "task_tag_cross_refs",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["taskId"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index("tagId")
    ] // Добавленные индексы
)
data class TaskTagCrossRef(
    val taskId: String,
    val tagId: String
)