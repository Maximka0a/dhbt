package com.example.dhbt.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.dhbt.R
import com.example.dhbt.presentation.MainViewModel
import com.example.dhbt.presentation.navigation.Dashboard
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    navController: NavController,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    var showDetailedOnboarding by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !showDetailedOnboarding,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        WelcomeScreen(
            onContinueClick = { showDetailedOnboarding = true },
            onSkipClick = {
                mainViewModel.completeOnboarding()
                navController.navigate(Dashboard) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
        )
    }

    AnimatedVisibility(
        visible = showDetailedOnboarding,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        DetailedOnboardingScreen(
            onFinish = {
                mainViewModel.completeOnboarding()
                navController.navigate(Dashboard) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
        )
    }
}

@Composable
fun WelcomeScreen(
    onContinueClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Добавлен отступ для статус-бара
            .navigationBarsPadding() // Добавлен отступ для навигационной панели
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        Text(
            text = "Добро пожаловать в DHBT",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ваш личный ассистент для улучшения продуктивности",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Feature Cards
        FeatureCard(
            icon = Icons.Outlined.AccountBox,
            title = "Привычки",
            description = "Формируйте полезные привычки и отслеживайте прогресс"
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.Outlined.Favorite,
            title = "Задачи",
            description = "Планируйте задачи и не забывайте о важных делах"
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.AutoMirrored.Outlined.List,
            title = "Матрица Эйзенхауэра",
            description = "Приоритизируйте задачи по важности и срочности"
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.Outlined.Build,
            title = "Помодоро таймер",
            description = "Повышайте концентрацию с методом помодоро"
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSkipClick) {
                Text("Пропустить")
            }

            Button(
                onClick = onContinueClick,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text("Продолжить")
            }
        }
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DetailedOnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Привычки",
            description = "Формируйте полезные привычки, отслеживайте свой прогресс и получайте награды за регулярность. Настраивайте напоминания и визуализируйте свой путь к здоровым привычкам.",
            lottieRes = R.raw.habits_animation
        ),
        OnboardingPage(
            title = "Задачи",
            description = "Планируйте свой день, создавайте задачи с приоритетами, сроками и категориями. Получайте уведомления о предстоящих делах и отмечайте выполненные задачи.",
            lottieRes = R.raw.tasks_animation
        ),
        OnboardingPage(
            title = "Матрица Эйзенхауэра",
            description = "Распределяйте задачи по четырем квадрантам в зависимости от их срочности и важности. Это поможет вам сосредоточиться на том, что действительно имеет значение.",
            lottieRes = R.raw.matrix_animation
        ),
        OnboardingPage(
            title = "Помодоро таймер",
            description = "Используйте технику Помодоро для повышения концентрации: работайте 25 минут, затем делайте короткий перерыв. Настраивайте интервалы под свои потребности.",
            lottieRes = R.raw.pomodoro_animation
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Добавлен отступ для статус-бара
            .navigationBarsPadding() // Добавлен отступ для навигационной панели
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            OnboardingPageContent(
                page = pages[page]
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Dots indicator
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(10.dp)
                            .background(
                                color = color,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }

            // Continue Button
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.size - 1) "Продолжить" else "Завершить"
                )
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(page.lottieRes))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier
                .size(280.dp)
                .padding(bottom = 24.dp)
        )

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val lottieRes: Int
)