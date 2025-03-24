package com.example.dhbt.presentation.subscription

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.R
import com.example.dhbt.domain.model.UserData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val userData by viewModel.userData.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val showPaymentFlow by viewModel.showPaymentFlow.collectAsState()
    val paymentFlowStep by viewModel.paymentFlowStep.collectAsState()
    val promotions by viewModel.promotions.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Форматтер для цен
    val priceFormat = remember { NumberFormat.getCurrencyInstance(Locale("ru", "RU")) }

    // Отображение сообщений успеха/ошибки
    LaunchedEffect(error, success) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        success?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Премиум-подписка") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (userData.isPremium) {
                // Показываем информацию для пользователя с премиум-статусом
                PremiumUserContent(
                    userData = userData,
                    onCancelSubscription = { viewModel.cancelSubscription() }
                )
            } else {
                // Показываем информацию для обычного пользователя
                SubscriptionContent(
                    selectedPlan = selectedPlan,
                    onPlanSelected = { viewModel.selectPlan(it) },
                    onSubscribe = { viewModel.startSubscription() },
                    onRestorePurchases = { viewModel.restorePurchases() },
                    promotions = promotions,
                    isLoading = isLoading
                )
            }

            // Диалог подтверждения подписки
            if (showConfirmDialog) {
                ConfirmSubscriptionDialog(
                    plan = selectedPlan,
                    onConfirm = { viewModel.confirmSubscription() },
                    onDismiss = { viewModel.cancelConfirmation() },
                    priceFormat = priceFormat
                )
            }

            // Диалог процесса оплаты
            if (showPaymentFlow) {
                PaymentFlowDialog(
                    step = paymentFlowStep,
                    onDismissRequest = { /* Пользователь не может закрыть этот диалог */ }
                )
            }

            // Индикатор загрузки
            if (isLoading && !showPaymentFlow) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumUserContent(
    userData: UserData,
    onCancelSubscription: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val validUntil = userData.premiumExpiryDate?.let {
        if (it == Long.MAX_VALUE) "∞" else dateFormat.format(Date(it))
    } ?: "∞"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = null,
            modifier = Modifier
                .size(180.dp)
                .padding(16.dp)
        )

        Text(
            text = "Вы Premium-пользователь!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Спасибо, что поддерживаете наше приложение",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ваша подписка активна до",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = validUntil,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Доступные премиум-функции",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Список доступных премиум-функций
        premiumFeatures.filter { !it.isAvailableInFree }.forEach { feature ->
            PremiumFeatureItem(
                feature = feature,
                isAvailable = true
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onCancelSubscription,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Text("Отменить подписку (тест)")
        }
    }
}

@Composable
fun SubscriptionContent(
    selectedPlan: SubscriptionPlan,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    onSubscribe: () -> Unit,
    onRestorePurchases: () -> Unit,
    promotions: List<Promotion>,
    isLoading: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Изображение баннера премиум
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = "Премиум",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp)
                )

                Text(
                    text = "DHbt Premium",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Раскройте весь потенциал продуктивности",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Промо-акции (если есть)
        if (promotions.isNotEmpty()) {
            PromotionsSection(
                promotions = promotions,
                onPromoSelected = { promo ->
                    // Выбираем соответствующий план
                    onPlanSelected(promo.planType)
                }
            )
        }

        // Выбор плана подписки
        SubscriptionPlansSection(
            selectedPlan = selectedPlan,
            onPlanSelected = onPlanSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка подписки
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = onSubscribe,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                Text(
                    text = "Оформить подписку",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onRestorePurchases,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Восстановить покупки")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Раздел с преимуществами
        PremiumFeaturesSection()

        Spacer(modifier = Modifier.height(24.dp))

        // Таблица сравнения
        ComparisonTableSection()

        Spacer(modifier = Modifier.height(24.dp))

        // FAQ секция
        FaqSection()

        Spacer(modifier = Modifier.height(32.dp))

        // Текст с условиями подписки
        Text(
            text = "Оформляя подписку, вы принимаете условия пользовательского соглашения. " +
                    "Подписка будет автоматически продлеваться, пока вы ее не отмените.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PromotionsSection(
    promotions: List<Promotion>,
    onPromoSelected: (Promotion) -> Unit
) {
    val currentTime = System.currentTimeMillis()
    val dateFormatter = SimpleDateFormat("dd.MM", Locale.getDefault())

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(promotions) { promotion ->
            // Проверка, что промо ещё активно
            if (promotion.validUntil > currentTime) {
                val validUntilDate = dateFormatter.format(Date(promotion.validUntil))

                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .clickable { onPromoSelected(promotion) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (promotion.isFreeTrial)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (promotion.isFreeTrial)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(40.dp),
                                contentColor = if (promotion.isFreeTrial)
                                    MaterialTheme.colorScheme.onSecondary
                                else
                                    MaterialTheme.colorScheme.onTertiary
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (promotion.isFreeTrial) {
                                        Text(
                                            text = "TRIAL",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "-${promotion.discountPercent}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = promotion.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "до $validUntilDate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (promotion.isFreeTrial)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = promotion.description,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onPromoSelected(promotion) },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (promotion.isFreeTrial)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(
                                text = if (promotion.isFreeTrial) "Попробовать" else "Выбрать"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionPlansSection(
    selectedPlan: SubscriptionPlan,
    onPlanSelected: (SubscriptionPlan) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Выберите план",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly plan
        SubscriptionPlanCard(
            plan = SubscriptionPlan.MONTHLY,
            isSelected = selectedPlan == SubscriptionPlan.MONTHLY,
            onClick = { onPlanSelected(SubscriptionPlan.MONTHLY) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Yearly plan
        SubscriptionPlanCard(
            plan = SubscriptionPlan.YEARLY,
            isSelected = selectedPlan == SubscriptionPlan.YEARLY,
            onClick = { onPlanSelected(SubscriptionPlan.YEARLY) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Lifetime plan
        SubscriptionPlanCard(
            plan = SubscriptionPlan.LIFETIME,
            isSelected = selectedPlan == SubscriptionPlan.LIFETIME,
            onClick = { onPlanSelected(SubscriptionPlan.LIFETIME) }
        )
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val priceFormat = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio button
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Plan details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (plan.isBestValue) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "ЛУЧШАЯ ЦЕНА",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (plan.discount > 0) {
                    Text(
                        text = "Экономия ${plan.discount}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (plan == SubscriptionPlan.MONTHLY || plan == SubscriptionPlan.YEARLY) {
                    Text(
                        text = "${priceFormat.format(plan.monthlyPrice)} / месяц",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Price
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (plan.discount > 0) {
                    val oldPriceMonthly = plan.monthlyPrice / (1 - plan.discount / 100.0)
                    val oldPriceTotal = plan.fullPrice / (1 - plan.discount / 100.0)

                    if (plan != SubscriptionPlan.LIFETIME) {
                        Text(
                            text = priceFormat.format(oldPriceTotal),
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Text(
                    text = priceFormat.format(plan.fullPrice),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (plan == SubscriptionPlan.LIFETIME) {
                    Text(
                        text = "Разовый платёж",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else if (plan == SubscriptionPlan.YEARLY) {
                    Text(
                        text = "За 12 месяцев",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumFeaturesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Преимущества Premium",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Отображаем все премиум-функции
        premiumFeatures.forEach { feature ->
            PremiumFeatureItem(
                feature = feature,
                isAvailable = !feature.isAvailableInFree
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun PremiumFeatureItem(
    feature: PremiumFeature,
    isAvailable: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка функции
        Surface(
            shape = CircleShape,
            color = if (isAvailable)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = getIconForFeature(feature.iconResName),
                    contentDescription = null,
                    tint = if (isAvailable)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Название и описание
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (isAvailable)
                    MaterialTheme.colorScheme.onBackground
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Индикатор доступности
        Icon(
            imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ComparisonTableSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Сравнение планов",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Заголовок таблицы
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Функция",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Бесплатно",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(90.dp)
            )

            Text(
                text = "Premium",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(90.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Строки таблицы
        ComparisonRow("Количество задач", "До 10", "∞")
        ComparisonRow("Количество привычек", "До 5", "∞")
        ComparisonRow("Категории", "До 3", "∞")
        ComparisonRow("Расширенная статистика", "Нет", "Да")
        ComparisonRow("Матрица Эйзенхауэра", "Да", "Да")
        ComparisonRow("Дополнительные темы", "Нет", "Да")
        ComparisonRow("Облачное хранение", "Нет", "Да")
        ComparisonRow("Экспорт данных", "Нет", "Да")
        ComparisonRow("Виджеты", "Базовые", "Расширенные")
        ComparisonRow("Техническая поддержка", "Общая", "Приоритетная")
        ComparisonRow("Реклама", "Да", "Нет")
    }
}

@Composable
fun ComparisonRow(
    feature: String,
    freeValue: String,
    premiumValue: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Free value
        Box(
            modifier = Modifier.width(90.dp),
            contentAlignment = Alignment.Center
        ) {
            if (freeValue == "Да") {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (freeValue == "Нет") {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            } else {
                Text(
                    text = freeValue,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Premium value
        Box(
            modifier = Modifier.width(90.dp),
            contentAlignment = Alignment.Center
        ) {
            if (premiumValue == "Да") {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (premiumValue == "Нет") {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            } else {
                Text(
                    text = premiumValue,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Divider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun FaqSection() {
    val faqItems = listOf(
        "Как отменить подписку?" to "Подписку можно отменить в любое время через Google Play/App Store. После отмены вы сможете пользоваться премиум до конца оплаченного периода.",
        "Будут ли списания автоматическими?" to "Да, месячная и годовая подписки продлеваются автоматически. Списание происходит за 24 часа до окончания текущего периода.",
        "Могу ли я перенести подписку на другое устройство?" to "Да, ваша подписка привязана к аккаунту Google Play/App Store. Просто войдите в тот же аккаунт на другом устройстве и восстановите покупки.",
        "Что даёт пожизненная подписка?" to "Пожизненная подписка – это разовый платёж, который предоставляет вам все премиум-функции навсегда без дополнительных списаний."
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Часто задаваемые вопросы",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        faqItems.forEach { (question, answer) ->
            var expanded by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = question,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Text(
                                text = answer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmSubscriptionDialog(
    plan: SubscriptionPlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    priceFormat: NumberFormat
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Подтверждение подписки")
        },
        text = {
            val message = buildAnnotatedString {
                append("Вы выбрали ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(plan.title.lowercase())
                }
                append(" подписку за ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(priceFormat.format(plan.fullPrice))
                }

                if (plan != SubscriptionPlan.LIFETIME) {
                    if (plan == SubscriptionPlan.MONTHLY) {
                        append(" в месяц")
                    } else {
                        append(" в год (${priceFormat.format(plan.monthlyPrice)} в месяц)")
                    }
                    append(".\n\nПодписка будет автоматически продлеваться до момента отмены.")
                } else {
                    append(".\n\nЭто разовый платеж, дающий доступ к премиум-функциям навсегда.")
                }
            }

            Text(message)
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Подтвердить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun PaymentFlowDialog(
    step: Int,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Обработка оплаты",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(24.dp))

                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                PaymentStepItem(
                    step = 1,
                    currentStep = step,
                    title = "Авторизация платежа",
                    description = "Проверка платежных данных"
                )

                Spacer(modifier = Modifier.height(16.dp))

                PaymentStepItem(
                    step = 2,
                    currentStep = step,
                    title = "Обработка транзакции",
                    description = "Выполнение платежа"
                )

                Spacer(modifier = Modifier.height(16.dp))

                PaymentStepItem(
                    step = 3,
                    currentStep = step,
                    title = "Активация подписки",
                    description = "Настройка вашего аккаунта"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Пожалуйста, не закрывайте это окно",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PaymentStepItem(
    step: Int,
    currentStep: Int,
    title: String,
    description: String
) {
    val isCurrentStep = step == currentStep
    val isCompletedStep = step < currentStep

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Индикатор этапа
        Surface(
            shape = CircleShape,
            color = when {
                isCompletedStep -> MaterialTheme.colorScheme.primary
                isCurrentStep -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isCompletedStep -> MaterialTheme.colorScheme.onPrimary
                        isCurrentStep -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            AnimatedVisibility(
                visible = isCurrentStep,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isCurrentStep) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else if (isCompletedStep) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Вспомогательная функция для получения иконок по имени ресурса
@Composable
fun getIconForFeature(iconResName: String): ImageVector {
    return when (iconResName) {
        "ic_task_unlimited" -> Icons.Default.ListAlt
        "ic_statistics" -> Icons.Default.BarChart
        "ic_theme" -> Icons.Default.Palette
        "ic_cloud" -> Icons.Default.Cloud
        "ic_widget" -> Icons.Default.Widgets
        "ic_support" -> Icons.Default.SupportAgent
        "ic_task_basic" -> Icons.Default.CheckCircleOutline
        "ic_chart_basic" -> Icons.Default.ShowChart
        else -> Icons.Default.Star
    }
}