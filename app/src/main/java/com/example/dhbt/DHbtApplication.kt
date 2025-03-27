package com.example.dhbt

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DHbtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}