package com.example.dhbt.presentation.habit.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatsBarGraph(
    data: List<Float>,
    maxValue: Float,
    barColor: Color,
    labelColor: Color,
    labels: List<String>,
    showAllLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Анимация для высоты столбцов
    val animatedBarValues = data.map { value ->
        val animatedValue by animateFloatAsState(
            targetValue = value,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            label = "bar_value"
        )
        animatedValue
    }

    val density = LocalDensity.current
    val barWidthDp = if (data.size <= 10) 16.dp else 8.dp
    val barCornerRadiusDp = if (data.size <= 10) 4.dp else 2.dp

    val barWidthPx = with(density) { barWidthDp.toPx() }
    val barCornerRadiusPx = with(density) { barCornerRadiusDp.toPx() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // График
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Вычисляем пространство между барами на основе ширины холста
                val barSpacingPx = (canvasWidth - (barWidthPx * data.size)) / (data.size + 1)

                // Рисуем столбцы
                for (i in animatedBarValues.indices) {
                    val barHeight = if (maxValue > 0f) {
                        (animatedBarValues[i] / maxValue) * canvasHeight
                    } else {
                        0f
                    }

                    val barX = barSpacingPx + i * (barWidthPx + barSpacingPx)
                    val barY = canvasHeight - barHeight

                    // Рисуем столбец
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(barX, barY),
                        size = Size(barWidthPx, barHeight),
                        cornerRadius = CornerRadius(barCornerRadiusPx, barCornerRadiusPx)
                    )
                }
            }
        }

        // Метки внизу
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            for (i in labels.indices) {
                // Отображаем либо все метки, либо только некоторые для большого количества данных
                if (showAllLabels ||
                    data.size <= 10 ||
                    i % (data.size / 5 + 1) == 0 ||  // +1 предотвращает деление на ноль
                    i == 0 ||
                    i == labels.lastIndex) {
                    Text(
                        text = labels[i],
                        color = labelColor,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(20.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
        }
    }
}