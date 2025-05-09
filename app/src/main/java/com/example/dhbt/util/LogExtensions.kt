package com.example.dhbt.presentation.util

import android.util.Log

/**
 * Уровень логирования для приложения.
 * В релизной версии можно установить NO_LOGS, чтобы отключить все логи.
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    NO_LOGS
}

/**
 * Текущий уровень логирования. Изменяйте его в зависимости от конфигурации сборки.
 */
var currentLogLevel = LogLevel.DEBUG

/**
 * Логирование с уровнем DEBUG.
 */
fun logDebug(tag: String, message: String, throwable: Throwable? = null) {
    if (currentLogLevel.ordinal <= LogLevel.DEBUG.ordinal) {
        if (throwable != null) {
            Log.d(tag, message, throwable)
        } else {
            Log.d(tag, message)
        }
    }
}

/**
 * Логирование с уровнем ERROR.
 */
fun logError(tag: String, message: String, throwable: Throwable? = null) {
    if (currentLogLevel.ordinal <= LogLevel.ERROR.ordinal) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}

/**
 * Логирование с уровнем INFO.
 */
fun logInfo(tag: String, message: String) {
    if (currentLogLevel.ordinal <= LogLevel.INFO.ordinal) {
        Log.i(tag, message)
    }
}