package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.TagEntity
import com.example.dhbt.domain.model.Tag
import javax.inject.Inject

class TagMapper @Inject constructor() {

    fun mapFromEntity(entity: TagEntity): Tag {
        return Tag(
            id = entity.tagId,
            name = entity.name,
            color = entity.color
        )
    }

    fun mapToEntity(domain: Tag): TagEntity {
        return TagEntity(
            tagId = domain.id,
            name = domain.name,
            color = domain.color
        )
    }
}