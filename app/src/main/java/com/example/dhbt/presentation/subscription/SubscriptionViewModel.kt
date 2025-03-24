package com.example.dhbt.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.data.repository.FakeSubscriptionRepository
import com.example.dhbt.domain.model.SubscriptionResult
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.repository.SubscriptionRepository
import com.example.dhbt.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    // Данные пользователя, объединённые с состоянием подписки
    val userData: StateFlow<UserData> = userPreferencesRepository.getUserData().combine(
        subscriptionRepository.getPremiumStatus()
    ) { userData, isPremium ->
        // Обновляем статус премиум в UserData на основе данных из репозитория подписки
        if (userData.isPremium != isPremium) {
            // Если статусы не совпадают, синхронизируем их
            val updatedData = userData.copy(
                isPremium = isPremium,
                premiumExpiryDate = null // Получим фактическую дату из другого потока
            )

            // Асинхронно сохраняем изменения
            viewModelScope.launch {
                userPreferencesRepository.updateUserData(updatedData)
            }

            updatedData
        } else {
            userData
        }
    }.combine(subscriptionRepository.getSubscriptionExpireDate()) { userData, expiryDate ->
        // Обновляем дату истечения, если она отличается
        if (userData.premiumExpiryDate != expiryDate) {
            val updatedData = userData.copy(premiumExpiryDate = expiryDate)

            // Асинхронно сохраняем изменения
            viewModelScope.launch {
                userPreferencesRepository.updateUserData(updatedData)
            }

            updatedData
        } else {
            userData
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserData()
    )

    // Выбранный план подписки
    private val _selectedPlan = MutableStateFlow(SubscriptionPlan.MONTHLY)
    val selectedPlan = _selectedPlan.asStateFlow()

    // Статус загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Сообщение об ошибке
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Статус успеха
    private val _success = MutableStateFlow<String?>(null)
    val success = _success.asStateFlow()

    // Показывать диалог подтверждения
    private val _showConfirmDialog = MutableStateFlow(false)
    val showConfirmDialog = _showConfirmDialog.asStateFlow()

    // Показывать имитацию процесса оплаты
    private val _showPaymentFlow = MutableStateFlow(false)
    val showPaymentFlow = _showPaymentFlow.asStateFlow()

    // Этап имитации оплаты (1-авторизация, 2-обработка, 3-проверка)
    private val _paymentFlowStep = MutableStateFlow(1)
    val paymentFlowStep = _paymentFlowStep.asStateFlow()

    // Список промо-акций
    private val _promotions = MutableStateFlow(generatePromotions())
    val promotions = _promotions.asStateFlow()

    // Действие при выборе плана подписки
    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    // Действие при нажатии на кнопку оформления подписки
    fun startSubscription() {
        _showConfirmDialog.value = true
    }

    // Действие при подтверждении подписки
    fun confirmSubscription() {
        _showConfirmDialog.value = false
        _showPaymentFlow.value = true
        _paymentFlowStep.value = 1
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Имитация процесса оплаты с анимацией этапов
                simulatePaymentFlow()

                // Определение параметров подписки
                val planId = when (selectedPlan.value) {
                    SubscriptionPlan.MONTHLY -> FakeSubscriptionRepository.PLAN_MONTHLY
                    SubscriptionPlan.YEARLY -> FakeSubscriptionRepository.PLAN_YEARLY
                    SubscriptionPlan.LIFETIME -> FakeSubscriptionRepository.PLAN_LIFETIME
                }

                // Микросы цены для API подписки
                val priceInMicros = (selectedPlan.value.fullPrice * 1_000_000).toLong()

                // Покупка подписки через репозиторий
                val result = subscriptionRepository.purchaseSubscription(
                    planId = planId,
                    priceAmountMicros = priceInMicros,
                    isLifetime = selectedPlan.value == SubscriptionPlan.LIFETIME
                )

                handleSubscriptionResult(result)

            } catch (e: Exception) {
                _error.value = "Произошла непредвиденная ошибка: ${e.message}"
            } finally {
                _showPaymentFlow.value = false
                _isLoading.value = false
            }
        }
    }

    // Имитация этапов оплаты для более реалистичного UX
    private suspend fun simulatePaymentFlow() {
        // Этап 1: Авторизация платежа
        _paymentFlowStep.value = 1
        kotlinx.coroutines.delay(1000)

        // Этап 2: Обработка платежа
        _paymentFlowStep.value = 2
        kotlinx.coroutines.delay(1200)

        // Этап 3: Проверка и активация
        _paymentFlowStep.value = 3
        kotlinx.coroutines.delay(800)
    }

    // Действие при отмене подтверждения
    fun cancelConfirmation() {
        _showConfirmDialog.value = false
    }

    // Действие при восстановлении покупок
    fun restorePurchases() {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Имитация задержки сети для лучшего UX
                kotlinx.coroutines.delay(1500)

                // Восстановление покупок через фейковый репозиторий
                val result = subscriptionRepository.restorePurchases()
                handleSubscriptionResult(result)

            } catch (e: Exception) {
                _error.value = "Ошибка при восстановлении подписки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Отмена подписки (для тестирования)
    fun cancelSubscription() {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = subscriptionRepository.cancelSubscription()
                when (result) {
                    is SubscriptionResult.Success -> {
                        _success.value = "Подписка успешно отменена"
                    }
                    is SubscriptionResult.Error -> {
                        _error.value = result.message
                    }
                    else -> {
                        _error.value = "Не удалось отменить подписку"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Ошибка при отмене подписки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Обработка результата подписки
    private fun handleSubscriptionResult(result: SubscriptionResult) {
        when (result) {
            is SubscriptionResult.Success -> {
                // Обновляем статус пользователя с премиум доступом
                viewModelScope.launch {
                    userPreferencesRepository.updateUserData(
                        userData.value.copy(
                            isPremium = true,
                            premiumExpiryDate = result.expiryDate
                        )
                    )
                }
                _success.value = "Подписка успешно оформлена!"
            }
            is SubscriptionResult.Error -> {
                _error.value = result.message
            }
            is SubscriptionResult.Cancelled -> {
                _error.value = "Оплата была отменена"
            }
            is SubscriptionResult.NoActiveSubscriptions -> {
                _error.value = "Активных подписок не найдено"
            }
        }
    }

    // Очистить сообщения об ошибках и успехе
    fun clearMessages() {
        _error.value = null
        _success.value = null
    }

    // Генерация тестовых промо-акций
    private fun generatePromotions(): List<Promotion> {
        return listOf(
            Promotion(
                id = "promo1",
                title = "Годовая со скидкой 30%",
                description = "Только сегодня! Оформите годовую подписку со скидкой 30%",
                discountPercent = 30,
                validUntil = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000, // 2 дня
                planType = SubscriptionPlan.YEARLY
            ),
            Promotion(
                id = "promo2",
                title = "Пробный период",
                description = "Попробуйте премиум бесплатно в течение 7 дней",
                discountPercent = 100,
                validUntil = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000, // 7 дней
                planType = SubscriptionPlan.MONTHLY,
                isFreeTrial = true
            )
        )
    }

    // Для тестирования - непосредственно установить подписку
    fun setTestSubscription(type: String, durationMonths: Int = 1) {
        if (subscriptionRepository is FakeSubscriptionRepository) {
            subscriptionRepository.setSubscriptionForTesting(
                planId = type,
                durationMonths = durationMonths
            )
        }
    }
}
// Модель данных для плана подписки
enum class SubscriptionPlan(
    val id: String,
    val title: String,
    val monthlyPrice: Double,
    val fullPrice: Double,
    val discount: Int = 0,
    val isBestValue: Boolean = false
) {
    MONTHLY(
        id = "monthly",
        title = "Месячная",
        monthlyPrice = 199.0,
        fullPrice = 199.0
    ),
    YEARLY(
        id = "yearly",
        title = "Годовая",
        monthlyPrice = 99.0,
        fullPrice = 1188.0,
        discount = 50,
        isBestValue = true
    ),
    LIFETIME(
        id = "lifetime",
        title = "Пожизненная",
        monthlyPrice = 0.0,
        fullPrice = 4990.0
    )
}

// Модель данных для промо-акций
data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val discountPercent: Int,
    val validUntil: Long,
    val planType: SubscriptionPlan,
    val isFreeTrial: Boolean = false
)

// Модель данных для функций премиум-аккаунта
data class PremiumFeature(
    val id: String,
    val title: String,
    val description: String,
    val iconResName: String,
    val isAvailableInFree: Boolean = false
)

// Список функций премиум-аккаунта
val premiumFeatures = listOf(
    PremiumFeature(
        id = "unlimited_tasks",
        title = "Неограниченное количество задач",
        description = "Создавайте сколько угодно задач и привычек",
        iconResName = "ic_task_unlimited",
        isAvailableInFree = false
    ),
    PremiumFeature(
        id = "advanced_statistics",
        title = "Расширенная статистика",
        description = "Глубокий анализ продуктивности и привычек",
        iconResName = "ic_statistics",
        isAvailableInFree = false
    ),
    PremiumFeature(
        id = "themes",
        title = "Дополнительные темы",
        description = "Более 10 уникальных тем оформления",
        iconResName = "ic_theme",
        isAvailableInFree = false
    ),
    PremiumFeature(
        id = "backups",
        title = "Облачное хранение",
        description = "Автоматическое резервное копирование данных",
        iconResName = "ic_cloud",
        isAvailableInFree = false
    ),
    PremiumFeature(
        id = "widgets",
        title = "Настраиваемые виджеты",
        description = "Виджеты для главного экрана устройства",
        iconResName = "ic_widget",
        isAvailableInFree = false
    ),
    PremiumFeature(
        id = "priority",
        title = "Приоритетная поддержка",
        description = "Быстрая помощь от разработчиков",
        iconResName = "ic_support",
        isAvailableInFree = false
    ),
    PremiumFeature(
        id = "basic_tasks",
        title = "Базовые задачи и привычки",
        description = "Создание и отслеживание основных задач",
        iconResName = "ic_task_basic",
        isAvailableInFree = true
    ),
    PremiumFeature(
        id = "basic_statistics",
        title = "Базовая статистика",
        description = "Простой обзор вашего прогресса",
        iconResName = "ic_chart_basic",
        isAvailableInFree = true
    )
)