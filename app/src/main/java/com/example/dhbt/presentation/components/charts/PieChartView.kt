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
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun PieChartView(
    data: List<PieEntry>,
    centerText: String = "",
    showLegend: Boolean = true,
    usePercentValues: Boolean = true,
    colors: List<Int> = ColorTemplate.MATERIAL_COLORS.toList(),
    holeRadius: Float = 40f,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Используем mutableStateOf для переменных, которые будут изменяться
    var chartInstance by remember { mutableStateOf<PieChart?>(null) }

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
            PieChart(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                setTransparentCircleAlpha(110)
                setTransparentCircleColor(Color.WHITE)
                this.holeRadius = holeRadius  // Используем this для доступа к свойствам объекта
                setDrawCenterText(true)
                rotationAngle = 0f
                isRotationEnabled = true
                isHighlightPerTapEnabled = true
                setCenterTextSize(16f)
                legend.isEnabled = showLegend

                // Сохраняем экземпляр графика для дальнейшего использования
                chartInstance = this
            }
        },
        update = { chart ->
            // Настройка данных
            chart.centerText = centerText
            chart.setUsePercentValues(usePercentValues)

            // Создаём датасет
            val dataSet = PieDataSet(data, "").apply {
                // Исправляем метод setColors - нужно преобразовать List<Int> в IntArray
                setColors(colors.toIntArray(), 255)
                valueTextColor = Color.BLACK
                valueTextSize = 14f
                sliceSpace = 3f
            }

            // Создание данных для графика
            val pieData = PieData(dataSet).apply {
                if (usePercentValues) {
                    setValueFormatter(PercentFormatter(chart))
                }
                setValueTextSize(12f)
                setValueTextColor(Color.BLACK)
            }

            chart.data = pieData
            chart.animateY(1000)
            chart.invalidate()
        }
    )
}