package com.example.dhbt.domain.model

data class Quote(
    val id: String,
    val text: String,
    val author: String? = null,
    val category: String? = null
)