package com.example.dhbt.data.repository

import com.example.dhbt.data.local.dao.QuoteDao
import com.example.dhbt.data.mapper.QuoteMapper
import com.example.dhbt.domain.model.Quote
import com.example.dhbt.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao,
    private val quoteMapper: QuoteMapper
) : QuoteRepository {

    override fun getAllQuotes(): Flow<List<Quote>> {
        return quoteDao.getAllQuotes().map { entities ->
            entities.map { quoteMapper.mapFromEntity(it) }
        }
    }

    override fun getQuotesByCategory(category: String): Flow<List<Quote>> {
        return quoteDao.getQuotesByCategory(category).map { entities ->
            entities.map { quoteMapper.mapFromEntity(it) }
        }
    }

    override suspend fun getRandomQuote(): Quote? {
        val entity = quoteDao.getRandomQuote() ?: return null
        return quoteMapper.mapFromEntity(entity)
    }

    override suspend fun getRandomQuoteByCategory(category: String): Quote? {
        val entity = quoteDao.getRandomQuoteByCategory(category) ?: return null
        return quoteMapper.mapFromEntity(entity)
    }

    override suspend fun addQuote(quote: Quote): String {
        val entity = quoteMapper.mapToEntity(quote)
        quoteDao.insertQuote(entity)
        return quote.id
    }

    override suspend fun addQuotes(quotes: List<Quote>) {
        val entities = quotes.map { quoteMapper.mapToEntity(it) }
        quoteDao.insertQuotes(entities)
    }

    override suspend fun updateQuote(quote: Quote) {
        val entity = quoteMapper.mapToEntity(quote)
        quoteDao.updateQuote(entity)
    }

    override suspend fun deleteQuote(quoteId: String) {
        quoteDao.deleteQuoteById(quoteId)
    }
}