package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey
    val quoteId: String = UUID.randomUUID().toString(),
    val text: String,
    val author: String? = null,
    val category: String? = null
)