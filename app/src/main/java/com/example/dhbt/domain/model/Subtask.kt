package com.example.dhbt.domain.model

data class Subtask(
    val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val completionDate: Long? = null,
    val order: Int = 0
)