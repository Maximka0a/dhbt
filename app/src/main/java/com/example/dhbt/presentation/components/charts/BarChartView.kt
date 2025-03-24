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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter

@Composable
fun BarChartView(
    data: List<BarEntry>,
    barColor: Int = Color.parseColor("#2196F3"),
    barWidth: Float = 0.7f,
    showValues: Boolean = true,
    animate: Boolean = true,
    xLabels: List<String> = emptyList(),
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Используем mutableStateOf для переменных, которые будут изменяться
    var chartInstance by remember { mutableStateOf<BarChart?>(null) }

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
            BarChart(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                description.isEnabled = false
                legend.isEnabled = false
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

                // Сохраняем экземпляр графика
                chartInstance = this
            }
        },
        update = { chart ->
            // Создаём датасет
            val dataSet = BarDataSet(data, "").apply {
                this.color = barColor  // Используем this для доступа к свойствам объекта
                setDrawValues(showValues)
                valueTextSize = 10f
                valueTextColor = Color.BLACK
            }

            // Подготовка данных для графика
            val barData = BarData(dataSet).apply {
                this.barWidth = barWidth  // Используем this для доступа к свойствам объекта
            }

            chart.data = barData

            // Анимация
            if (animate) {
                chart.animateY(1000)
            }

            chart.invalidate()
        }
    )
}