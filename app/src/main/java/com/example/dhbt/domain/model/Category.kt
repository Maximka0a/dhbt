package com.example.dhbt.domain.model

data class Category(
    val id: String,
    val name: String,
    val color: String? = null,
    val iconEmoji: String? = null,
    val type: CategoryType,
    val order: Int = 0
)

enum class CategoryType(val value: Int) {
    TASK(0),
    HABIT(1),
    BOTH(2);

    companion object {
        fun fromInt(value: Int): CategoryType = values().firstOrNull { it.value == value } ?: BOTH
    }
}