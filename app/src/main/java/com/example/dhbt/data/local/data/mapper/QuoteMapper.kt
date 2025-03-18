package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.QuoteEntity
import com.example.dhbt.domain.model.Quote
import javax.inject.Inject

class QuoteMapper @Inject constructor() {

    fun mapFromEntity(entity: QuoteEntity): Quote {
        return Quote(
            id = entity.quoteId,
            text = entity.text,
            author = entity.author,
            category = entity.category
        )
    }

    fun mapToEntity(domain: Quote): QuoteEntity {
        return QuoteEntity(
            quoteId = domain.id,
            text = domain.text,
            author = domain.author,
            category = domain.category
        )
    }
}