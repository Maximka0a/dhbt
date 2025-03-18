package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.SubtaskEntity
import com.example.dhbt.domain.model.Subtask
import javax.inject.Inject

class SubtaskMapper @Inject constructor() {

    fun mapFromEntity(entity: SubtaskEntity): Subtask {
        return Subtask(
            id = entity.subtaskId,
            taskId = entity.taskId,
            title = entity.title,
            isCompleted = entity.isCompleted,
            completionDate = entity.completionDate,
            order = entity.order
        )
    }

    fun mapToEntity(domain: Subtask): SubtaskEntity {
        return SubtaskEntity(
            subtaskId = domain.id,
            taskId = domain.taskId,
            title = domain.title,
            isCompleted = domain.isCompleted,
            completionDate = domain.completionDate,
            order = domain.order
        )
    }
}