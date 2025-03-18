package com.example.dhbt.domain.model

data class HabitTracking(
    val id: String,
    val habitId: String,
    val date: Long,
    val isCompleted: Boolean = false,
    val value: Float? = null,
    val duration: Int? = null,
    val notes: String? = null
)