package com.example.dhbt.presentation.components.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Кастомный View для отображения тепловой карты календаря активности в стиле GitHub contributions
 */
class CalendarHeatmapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: Map<LocalDate, Float> = mapOf()
    private var startDate: LocalDate = LocalDate.now().minusMonths(5).withDayOfMonth(1)
    private var endDate: LocalDate = LocalDate.now()

    private val cellSize = context.resources.displayMetrics.density * 12 // 12dp
    private val cellMargin = context.resources.displayMetrics.density * 2 // 2dp

    private val emptyCellColor = Color.parseColor("#EEEEEE")
    private val lowColor = Color.parseColor("#D6E685")
    private val mediumColor = Color.parseColor("#8CC665")
    private val highColor = Color.parseColor("#44A340")
    private val maxColor = Color.parseColor("#1E6823")

    private val cellPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#666666")
        textSize = context.resources.displayMetrics.density * 10 // 10sp
        textAlign = Paint.Align.CENTER
    }

    fun setData(data: Map<LocalDate, Float>) {
        this.data = data
        invalidate()
    }

    fun setDateRange(startDate: LocalDate, endDate: LocalDate) {
        this.startDate = startDate
        this.endDate = endDate
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val weekFields = WeekFields.of(Locale.getDefault())
        var currentDate = startDate

        // Определяем количество недель для отображения
        val weeks = ((endDate.toEpochDay() - startDate.toEpochDay()) / 7).toInt() + 2

        // Определяем Y-позиции для дней недели
        val dayYPositions = Array(7) { idx ->
            idx * (cellSize + cellMargin) + cellSize / 2
        }

        // Рисуем дни недели (сокращенные названия)
        val dayNames = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        for (i in 0..6) {
            canvas.drawText(
                dayNames[i],
                cellSize / 2,
                dayYPositions[i] + textPaint.textSize / 3,
                textPaint
            )
        }

        // Рисуем ячейки календаря
        var weekCount = 0
        val cellRect = RectF()

        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            // Определяем день недели (в Java/Kotlin 1 = понедельник, 7 = воскресенье)
            val dayOfWeek = currentDate.dayOfWeek.value - 1 // 0 = понедельник, 6 = воскресенье

            // Определяем X-позицию недели (добавляем отступ для названий дней недели)
            val weekX = weekCount * (cellSize + cellMargin) + cellSize + cellMargin * 2

            // Определяем Y-позицию для текущего дня недели
            val dayY = dayOfWeek * (cellSize + cellMargin)

            // Определяем цвет ячейки на основе данных
            val intensity = data[currentDate] ?: 0f
            cellPaint.color = when {
                intensity <= 0f -> emptyCellColor
                intensity < 0.25f -> lowColor
                intensity < 0.5f -> mediumColor
                intensity < 0.75f -> highColor
                else -> maxColor
            }

            // Рисуем ячейку
            cellRect.set(weekX, dayY, weekX + cellSize, dayY + cellSize)
            canvas.drawRoundRect(cellRect, 2f, 2f, cellPaint)

            // Переходим к следующему дню
            currentDate = currentDate.plusDays(1)

            // Если перешли на новую неделю
            if (currentDate.dayOfWeek == DayOfWeek.MONDAY) {
                weekCount++

                // Рисуем название месяца каждую 4-ю неделю
                if (weekCount % 4 == 0) {
                    val monthName = currentDate.month.toString().substring(0, 3)
                    canvas.drawText(
                        monthName,
                        weekX + cellSize / 2,
                        dayYPositions[0] - cellSize,
                        textPaint
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarHeatmap(
    data: Map<LocalDate, Float>,
    startDate: LocalDate = LocalDate.now().minusMonths(5).withDayOfMonth(1),
    endDate: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CalendarHeatmapView(context).apply {
                setDateRange(startDate, endDate)
                setData(data)
            }
        },
        update = { view ->
            view.setDateRange(startDate, endDate)
            view.setData(data)
        }
    )
}