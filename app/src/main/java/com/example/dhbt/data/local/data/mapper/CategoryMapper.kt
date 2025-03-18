package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.CategoryEntity
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import javax.inject.Inject

class CategoryMapper @Inject constructor() {

    fun mapFromEntity(entity: CategoryEntity): Category {
        return Category(
            id = entity.categoryId,
            name = entity.name,
            color = entity.color,
            iconEmoji = entity.iconEmoji,
            type = CategoryType.fromInt(entity.type),
            order = entity.order
        )
    }

    fun mapToEntity(domain: Category): CategoryEntity {
        return CategoryEntity(
            categoryId = domain.id,
            name = domain.name,
            color = domain.color,
            iconEmoji = domain.iconEmoji,
            type = domain.type.value,
            order = domain.order
        )
    }
}