package com.example.dhbt.util

import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.model.UserPreferences
import timber.log.Timber

/**
 * Утилитарный класс с функциями расширения для детального логирования объектов
 */
object LogUtils {

    /**
     * Расширение для UserPreferences для построчного логирования всех полей
     */
    fun UserPreferences.logDetailed(tag: String = "UserPreferences") {
        Timber.tag(tag).d("===== НАЧАЛО ДАННЫХ PREFERENCES =====")
        Timber.tag(tag).d("hasCompletedOnboarding: $hasCompletedOnboarding")
        Timber.tag(tag).d("language: $language")
        Timber.tag(tag).d("theme: $theme (${theme.value})")
        Timber.tag(tag).d("startScreenType: $startScreenType (${startScreenType.value})")
        Timber.tag(tag).d("quietHoursEnabled: $quietHoursEnabled")
        Timber.tag(tag).d("quietHoursStart: $quietHoursStart")
        Timber.tag(tag).d("quietHoursEnd: $quietHoursEnd")
        Timber.tag(tag).d("===== КОНЕЦ ДАННЫХ PREFERENCES =====")
    }

    /**
     * Расширение для UserData для построчного логирования всех полей
     */
    fun UserData.logDetailed(tag: String = "UserData") {
        Timber.tag(tag).d("===== НАЧАЛО ДАННЫХ USER =====")
        Timber.tag(tag).d("userId: $userId")
        Timber.tag(tag).d("name: $name")
        Timber.tag(tag).d("email: $email")
        Timber.tag(tag).d("avatarUrl: $avatarUrl")
        Timber.tag(tag).d("wakeUpTime: $wakeUpTime")
        Timber.tag(tag).d("sleepTime: $sleepTime")
        Timber.tag(tag).d("usageGoal: $usageGoal")
        Timber.tag(tag).d("isPremium: $isPremium")
        Timber.tag(tag).d("premiumExpiryDate: $premiumExpiryDate")
        Timber.tag(tag).d("customEmoji: $customEmoji")
        Timber.tag(tag).d("registrationDate: $registrationDate")
        Timber.tag(tag).d("lastLoginDate: $lastLoginDate")
        Timber.tag(tag).d("===== КОНЕЦ ДАННЫХ USER =====")
    }
}