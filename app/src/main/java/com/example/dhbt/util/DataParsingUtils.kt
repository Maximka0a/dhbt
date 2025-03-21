package com.example.dhbt.data.util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Утилитный класс для безопасного преобразования данных
 */
object DataParsingUtils {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    /**
     * Безопасно преобразует строку с числами в список целых чисел
     */
    fun parseCommaSeparatedIntegers(commaSeparatedString: String?): List<Int>? {
        if (commaSeparatedString.isNullOrBlank()) {
            return null
        }

        return try {
            commaSeparatedString.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.toInt() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Преобразует список целых чисел в строку с разделителями-запятыми
     */
    fun intListToCommaSeparatedString(list: List<Int>?): String? {
        return list?.joinToString(",")
    }

    /**
     * Безопасно десериализует JSON-строку в указанный тип
     */
    inline fun <reified T> safeJsonDecode(jsonString: String?): T? {
        if (jsonString.isNullOrBlank()) {
            return null
        }

        return try {
            json.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Безопасно сериализует объект в JSON-строку
     */
    inline fun <reified T> safeJsonEncode(obj: T?): String? {
        if (obj == null) {
            return null
        }

        return try {
            json.encodeToString(obj)
        } catch (e: Exception) {
            null
        }
    }
}