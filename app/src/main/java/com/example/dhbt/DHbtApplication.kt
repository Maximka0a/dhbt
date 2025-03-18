package com.example.dhbt

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DHbtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализация библиотеки для работы с датами
        AndroidThreeTen.init(this)
    }
}