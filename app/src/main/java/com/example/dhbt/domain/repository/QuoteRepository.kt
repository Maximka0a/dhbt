package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun getAllQuotes(): Flow<List<Quote>>
    fun getQuotesByCategory(category: String): Flow<List<Quote>>
    suspend fun getRandomQuote(): Quote?
    suspend fun getRandomQuoteByCategory(category: String): Quote?

    suspend fun addQuote(quote: Quote): String
    suspend fun addQuotes(quotes: List<Quote>)
    suspend fun updateQuote(quote: Quote)
    suspend fun deleteQuote(quoteId: String)
}