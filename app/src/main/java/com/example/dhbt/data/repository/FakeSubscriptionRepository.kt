package com.example.dhbt.data.repository

import com.example.dhbt.domain.model.SubscriptionResult
import com.example.dhbt.domain.repository.SubscriptionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Random
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeSubscriptionRepository @Inject constructor() : SubscriptionRepository {

    // Эмуляция хранилища данных
    private val _subscriptionData = MutableStateFlow<SubscriptionData?>(null)

    // Статические значения для разных типов подписок
    companion object {
        const val PLAN_MONTHLY = "monthly"
        const val PLAN_YEARLY = "yearly"
        const val PLAN_LIFETIME = "lifetime"

        const val MONTHLY_DURATION_MS = 30L * 24 * 60 * 60 * 1000  // 30 дней
        const val YEARLY_DURATION_MS = 365L * 24 * 60 * 60 * 1000  // 365 дней

        // Вероятность симуляции ошибки в запросах
        const val ERROR_PROBABILITY = 0.1 // 10%
    }

    override fun getPremiumStatus(): Flow<Boolean> {
        return _subscriptionData.map { data ->
            if (data == null) return@map false

            val now = System.currentTimeMillis()
            return@map data.expiryDate == null || data.expiryDate > now
        }
    }

    override fun getSubscriptionExpireDate(): Flow<Long?> {
        return _subscriptionData.map { it?.expiryDate }
    }

    override suspend fun checkHasActiveSubscription(): Boolean {
        val data = _subscriptionData.value ?: return false
        val now = System.currentTimeMillis()
        return data.expiryDate == null || data.expiryDate > now
    }

    override suspend fun purchaseSubscription(
        planId: String,
        priceAmountMicros: Long,
        isLifetime: Boolean
    ): SubscriptionResult {
        // Симуляция задержки сети
        delay(1500)

        // Симуляция случайной ошибки
        if (Random().nextDouble() < ERROR_PROBABILITY) {
            return SubscriptionResult.Error("Ошибка обработки платежа. Пожалуйста, попробуйте ещё раз.")
        }

        // Симуляция отмены пользователем (с малой вероятностью для тестирования)
        if (Random().nextDouble() < 0.05) {
            return SubscriptionResult.Cancelled
        }

        // Вычисление даты истечения подписки
        val now = System.currentTimeMillis()
        val expiryDate = when (planId) {
            PLAN_MONTHLY -> now + MONTHLY_DURATION_MS
            PLAN_YEARLY -> now + YEARLY_DURATION_MS
            PLAN_LIFETIME -> null // Пожизненная подписка
            else -> now + MONTHLY_DURATION_MS // По умолчанию месячная
        }

        // Создаем запись о подписке
        val subscriptionId = UUID.randomUUID().toString()
        val subscriptionData = SubscriptionData(
            id = subscriptionId,
            planId = planId,
            purchaseTime = now,
            expiryDate = expiryDate,
            isLifetime = isLifetime || planId == PLAN_LIFETIME
        )

        // Сохраняем данные подписки
        _subscriptionData.value = subscriptionData

        return SubscriptionResult.Success(
            subscriptionId = subscriptionId,
            expiryDate = expiryDate
        )
    }

    override suspend fun restorePurchases(): SubscriptionResult {
        // Симуляция задержки сети
        delay(1500)

        // Симуляция случайной ошибки
        if (Random().nextDouble() < ERROR_PROBABILITY) {
            return SubscriptionResult.Error("Не удалось восстановить покупки. Ошибка сети.")
        }

        // Проверка наличия активной подписки
        val currentSubscription = _subscriptionData.value
        if (currentSubscription != null) {
            val now = System.currentTimeMillis()

            // Проверяем срок действия подписки
            if (currentSubscription.isLifetime || (currentSubscription.expiryDate != null && currentSubscription.expiryDate > now)) {
                return SubscriptionResult.Success(
                    subscriptionId = currentSubscription.id,
                    expiryDate = currentSubscription.expiryDate
                )
            }
        }

        // Если подписка не найдена или истекла
        return SubscriptionResult.NoActiveSubscriptions
    }

    override suspend fun cancelSubscription(): SubscriptionResult {
        // Симуляция задержки сети
        delay(500)

        val currentSubscription = _subscriptionData.value ?:
        return SubscriptionResult.Error("Нет активной подписки для отмены")

        // Симуляция удачной отмены
        _subscriptionData.value = null
        return SubscriptionResult.Success(
            subscriptionId = currentSubscription.id,
            expiryDate = System.currentTimeMillis()  // Истекает сразу
        )
    }

    /**
     * Для тестирования - установить подписку напрямую
     */
    fun setSubscriptionForTesting(planId: String, durationMonths: Int) {
        val now = System.currentTimeMillis()
        val expiryDate = when {
            planId == PLAN_LIFETIME -> null
            else -> now + (durationMonths * 30L * 24 * 60 * 60 * 1000)
        }

        _subscriptionData.value = SubscriptionData(
            id = UUID.randomUUID().toString(),
            planId = planId,
            purchaseTime = now,
            expiryDate = expiryDate,
            isLifetime = planId == PLAN_LIFETIME
        )
    }

    /**
     * Данные подписки (внутренняя модель)
     */
    data class SubscriptionData(
        val id: String,
        val planId: String,
        val purchaseTime: Long,
        val expiryDate: Long?,
        val isLifetime: Boolean
    )
}