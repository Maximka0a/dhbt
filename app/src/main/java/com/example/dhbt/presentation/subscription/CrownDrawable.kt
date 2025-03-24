package com.example.dhbt.presentation.subscription

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.dhbt.R

/**
 * Drawable для отображения короны премиум-подписки
 * В реальном проекте можно заменить на настоящее векторное изображение
 */
class CrownDrawable(private val context: Context) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700") // Золотой цвет
        style = Paint.Style.FILL
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D4AF37") // Тёмный золотой
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val path = Path()

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        // Сбрасываем путь перед новым рисованием
        path.reset()

        // Рисуем основу короны
        path.moveTo(width * 0.1f, height * 0.7f)
        path.lineTo(width * 0.9f, height * 0.7f)
        path.lineTo(width * 0.8f, height * 0.9f)
        path.lineTo(width * 0.2f, height * 0.9f)
        path.lineTo(width * 0.1f, height * 0.7f)

        // Рисуем зубцы
        path.moveTo(width * 0.1f, height * 0.7f)
        path.lineTo(width * 0.2f, height * 0.3f)

        path.moveTo(width * 0.3f, height * 0.7f)
        path.lineTo(width * 0.5f, height * 0.1f)
        path.lineTo(width * 0.7f, height * 0.7f)

        path.moveTo(width * 0.9f, height * 0.7f)
        path.lineTo(width * 0.8f, height * 0.3f)

        // Рисуем драгоценные камни
        canvas.drawCircle(width * 0.2f, height * 0.3f, width * 0.05f, Paint().apply {
            color = Color.parseColor("#FF0000") // Красный
        })

        canvas.drawCircle(width * 0.5f, height * 0.1f, width * 0.06f, Paint().apply {
            color = Color.parseColor("#0000FF") // Синий
        })

        canvas.drawCircle(width * 0.8f, height * 0.3f, width * 0.05f, Paint().apply {
            color = Color.parseColor("#00FF00") // Зелёный
        })

        // Рисуем корону
        canvas.drawPath(path, paint)
        canvas.drawPath(path, outlinePaint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        outlinePaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        outlinePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}