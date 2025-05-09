package com.example.dhbt.util

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import timber.log.Timber
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "user_preferences"
    private const val KEY_LANGUAGE = "language"

    /**
     * Применяет локаль к контексту.
     * Также сохраняет локаль в SharedPreferences для использования при перезапуске.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        Timber.d("Установка локали: $languageCode")

        // Сначала сохраняем выбор языка в SharedPreferences
        saveLanguagePreference(context, languageCode)

        // Затем меняем локаль
        return updateResources(context, languageCode)
    }

    /**
     * Обновляет ресурсы с новой локалью
     */
    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Для Android 7.0 (Nougat) и выше
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)

            // Важно! Создаем новый контекст с обновленной конфигурацией
            return context.createConfigurationContext(config)
        } else {
            // Для более старых версий
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }

    /**
     * Сохраняет выбор языка в SharedPreferences
     */
    private fun saveLanguagePreference(context: Context, language: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString(KEY_LANGUAGE, language)
            .apply() // используем apply() вместо commit() для асинхронного сохранения
    }

    /**
     * Получает текущий язык из SharedPreferences
     */
    fun getLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_LANGUAGE, "ru") ?: "ru"
    }
}