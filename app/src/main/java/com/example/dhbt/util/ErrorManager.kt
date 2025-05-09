package com.example.dhbt.presentation.util

import androidx.annotation.StringRes
import com.example.dhbt.R
import com.github.mikephil.charting.BuildConfig

/**
 * Класс для управления локализованными ошибками
 */
object ErrorManager {
    /**
     * Типы ошибок приложения с соответствующими ресурсами строк
     */
    enum class ErrorType(@StringRes val messageResId: Int) {
        // Общие ошибки
        NETWORK_ERROR(R.string.error_network),
        DATABASE_ERROR(R.string.error_database),
        UNKNOWN_ERROR(R.string.error_unknown),

        // Ошибки задач
        TASK_UPDATE_ERROR(R.string.error_task_update),
        TASK_DELETE_ERROR(R.string.error_task_delete),

        // Ошибки привычек
        HABIT_PROGRESS_ERROR(R.string.error_habit_progress),
        HABIT_DATA_ERROR(R.string.error_habit_data),

        // Ошибки загрузки данных
        DATA_LOAD_ERROR(R.string.error_data_load)
    }

    /**
     * Представление ошибки для UI
     */
    data class UiError(
        val type: ErrorType,
        val details: String? = null,
        @StringRes val buttonTextResId: Int = R.string.action_dismiss
    )

    /**
     * Преобразует исключение в локализованную ошибку UI
     */
    fun fromException(e: Throwable, defaultType: ErrorType = ErrorType.UNKNOWN_ERROR): UiError {
        // Анализируем тип исключения и подбираем подходящую ошибку
        val errorType = when {
            e.message?.contains("database", ignoreCase = true) == true -> ErrorType.DATABASE_ERROR
            e.message?.contains("network", ignoreCase = true) == true -> ErrorType.NETWORK_ERROR
            e.message?.contains("habit", ignoreCase = true) == true -> ErrorType.HABIT_DATA_ERROR
            e.message?.contains("task", ignoreCase = true) == true -> ErrorType.TASK_UPDATE_ERROR
            else -> defaultType
        }

        return UiError(
            type = errorType,
            details = if (BuildConfig.DEBUG) e.message else null // детали ошибки только в режиме отладки
        )
    }
}