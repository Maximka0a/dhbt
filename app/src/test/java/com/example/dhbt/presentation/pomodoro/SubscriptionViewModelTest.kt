package com.example.dhbt.presentation.subscription

import com.example.dhbt.data.repository.FakeSubscriptionRepository
import com.example.dhbt.domain.model.SubscriptionResult
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.repository.SubscriptionRepository
import com.example.dhbt.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SubscriptionViewModelTest {

    @Mock
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Mock
    private lateinit var subscriptionRepository: SubscriptionRepository

    private lateinit var viewModel: SubscriptionViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val userDataFlow = MutableStateFlow(UserData())
    private val premiumStatusFlow = MutableStateFlow(false)
    private val expiryDateFlow = MutableStateFlow<Long?>(null)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Настройка моков
        whenever(userPreferencesRepository.getUserData()).thenReturn(userDataFlow)
        whenever(subscriptionRepository.getPremiumStatus()).thenReturn(premiumStatusFlow)
        whenever(subscriptionRepository.getSubscriptionExpireDate()).thenReturn(expiryDateFlow)

        // Создание ViewModel с моками
        viewModel = SubscriptionViewModel(userPreferencesRepository, subscriptionRepository)

        // Запускаем начальную инициализацию
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `выбор плана подписки обновляет выбранный план`() = runTest {
        // Действие
        viewModel.selectPlan(SubscriptionPlan.YEARLY)

        // Проверка
        assertEquals(SubscriptionPlan.YEARLY, viewModel.selectedPlan.value)
    }

    @Test
    fun `confirmSubscription запускает процесс платежа`() = runTest {
        // Настройка: подготовка успешного ответа от репозитория
        val successResult = SubscriptionResult.Success("sub-1", 1735689600000) // 01.01.2025
        whenever(subscriptionRepository.purchaseSubscription(any(), any(), any())).thenReturn(successResult)

        // Действие: запуск процесса подтверждения подписки
        viewModel.startSubscription() // Открываем диалог
        viewModel.confirmSubscription() // Подтверждаем

        // Запускаем все корутины
        testDispatcher.scheduler.advanceUntilIdle()

        // Проверка
        assertFalse(viewModel.showConfirmDialog.value) // Диалог закрыт
        assertFalse(viewModel.isLoading.value) // Загрузка завершена
        assertEquals("Подписка успешно оформлена!", viewModel.success.value) // Сообщение об успехе

        // Проверка вызова репозитория с правильными параметрами
        verify(subscriptionRepository).purchaseSubscription(
            planId = FakeSubscriptionRepository.PLAN_MONTHLY,
            priceAmountMicros = (SubscriptionPlan.MONTHLY.fullPrice * 1_000_000).toLong(),
            isLifetime = false
        )

        // Проверка обновления данных пользователя
        verify(userPreferencesRepository).updateUserData(any())
    }

    @Test
    fun `confirmSubscription обрабатывает ошибку подписки`() = runTest {
        // Настройка: подготовка ответа с ошибкой
        val errorResult = SubscriptionResult.Error("Ошибка оплаты")
        whenever(subscriptionRepository.purchaseSubscription(any(), any(), any())).thenReturn(errorResult)

        // Действие
        viewModel.startSubscription()
        viewModel.confirmSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        // Проверка
        assertFalse(viewModel.isLoading.value)
        assertEquals("Ошибка оплаты", viewModel.error.value)
        assertNull(viewModel.success.value)
    }

    @Test
    fun `cancelSubscription обрабатывает успешное отключение подписки`() = runTest {
        // Настройка
        val successResult = SubscriptionResult.Success("sub-cancelled", System.currentTimeMillis())
        whenever(subscriptionRepository.cancelSubscription()).thenReturn(successResult)

        // Действие
        viewModel.cancelSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        // Проверка
        assertFalse(viewModel.isLoading.value)
        assertEquals("Подписка успешно отменена", viewModel.success.value)
    }

}