package com.example.dhbt.presentation.components.charts

import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

@Composable
fun LineChartView(
    data: List<Entry>,
    lineColor: Int = Color.BLUE,
    lineWidth: Float = 2f,
    fillColor: Int = Color.parseColor("#4D2196F3"),
    showDots: Boolean = true,
    animator: ChartAnimator = ChartAnimator.EASE_IN,
    xLabels: List<String> = emptyList(),
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Используем mutableStateOf для переменных, которые будут изменяться
    var chartInstance by remember { mutableStateOf<LineChart?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                chartInstance?.clear()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                description.isEnabled = false
                legend.isEnabled = true
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(false)
                setPinchZoom(false)
                setDrawGridBackground(false)

                // Настройка осей
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)

                if (xLabels.isNotEmpty()) {
                    xAxis.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index >= 0 && index < xLabels.size) xLabels[index] else ""
                        }
                    }
                }

                axisLeft.setDrawGridLines(true)
                axisRight.isEnabled = false

                // Сохраняем экземпляр графика для дальнейшего использования
                chartInstance = this
            }
        },
        update = { chart ->
            // Обновляем данные графика
            val dataSet = LineDataSet(data, "").apply {
                this.color = lineColor  // Используем this для доступа к свойствам объекта
                this.lineWidth = lineWidth  // Используем this для доступа к свойствам объекта
                setDrawCircles(showDots)
                setCircleColor(lineColor)
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = Color.BLACK
                setDrawFilled(true)
                this.fillColor = fillColor  // Используем this для доступа к свойствам объекта
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }

            val lineData = LineData(dataSet)
            chart.data = lineData

            // Анимация
            when (animator) {
                ChartAnimator.NONE -> {}
                ChartAnimator.EASE_IN -> chart.animateX(1000)
                ChartAnimator.EASE_Y -> chart.animateY(1000)
                ChartAnimator.EASE_BOTH -> chart.animateXY(1000, 1000)
            }

            // Обновляем визуально
            chart.invalidate()
        }
    )
}

enum class ChartAnimator {
    NONE, EASE_IN, EASE_Y, EASE_BOTH
}