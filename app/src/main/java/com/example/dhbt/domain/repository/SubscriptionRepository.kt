package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.SubscriptionResult
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    /**
     * Текущий статус премиум-подписки
     */
    fun getPremiumStatus(): Flow<Boolean>

    /**
     * Дата истечения подписки (null если подписка пожизненная или отсутствует)
     */
    fun getSubscriptionExpireDate(): Flow<Long?>

    /**
     * Проверяет, имеет ли пользователь действующую подписку
     */
    suspend fun checkHasActiveSubscription(): Boolean

    /**
     * Покупка подписки
     */
    suspend fun purchaseSubscription(
        planId: String,
        priceAmountMicros: Long,
        isLifetime: Boolean = false
    ): SubscriptionResult

    /**
     * Восстановление покупок
     */
    suspend fun restorePurchases(): SubscriptionResult

    /**
     * Отмена подписки (для имитации в фейковой версии)
     */
    suspend fun cancelSubscription(): SubscriptionResult
}