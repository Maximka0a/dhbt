package com.example.dhbt.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dhbt.domain.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * BroadcastReceiver для обработки действий с привычками из уведомлений
 */
@AndroidEntryPoint
class HabitActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TRACK_HABIT = "com.example.dhbt.ACTION_TRACK_HABIT"
        const val ACTION_SKIP_HABIT = "com.example.dhbt.ACTION_SKIP_HABIT"
    }

    @Inject
    lateinit var habitRepository: HabitRepository

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habitId") ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TRACK_HABIT -> handleTrackHabit(habitId)
                    ACTION_SKIP_HABIT -> handleSkipHabit(habitId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке действия для привычки $habitId")
            }
        }
    }

    private suspend fun handleTrackHabit(habitId: String) {
        Timber.d("Отслеживание привычки $habitId из уведомления")
        try {
            habitRepository.incrementHabitProgress(habitId, System.currentTimeMillis())
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при отслеживании привычки $habitId")
        }
    }

    private suspend fun handleSkipHabit(habitId: String) {
        Timber.d("Пропуск привычки $habitId из уведомления")
        // Тут можно добавить логику для пропуска привычки
        // Например, запись о пропуске в базу данных
    }
}