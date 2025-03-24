package com.example.dhbt.domain.model

/**
 * Результат операции покупки подписки
 */
sealed class SubscriptionResult {
    /**
     * Покупка успешно завершена
     * @param subscriptionId идентификатор подписки
     * @param expiryDate дата истечения подписки или null для пожизненной
     */
    data class Success(
        val subscriptionId: String,
        val expiryDate: Long? = null
    ) : SubscriptionResult()

    /**
     * Покупка не завершена из-за ошибки
     * @param message сообщение об ошибке
     */
    data class Error(val message: String) : SubscriptionResult()

    /**
     * Пользователь отменил покупку
     */
    object Cancelled : SubscriptionResult()

    /**
     * Не найдено активных подписок (для восстановления покупок)
     */
    object NoActiveSubscriptions : SubscriptionResult()
}