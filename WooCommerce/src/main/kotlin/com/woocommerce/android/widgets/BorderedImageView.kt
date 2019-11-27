package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import org.wordpress.android.util.DisplayUtils

class BorderedImageView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : AppCompatImageView(ctx, attrs) {
    private val borderPaint: Paint
    private val padding: Float
    private val strokeWidth: Float

    init {
        padding = DisplayUtils.dpToPx(context, 2).toFloat()
        strokeWidth = DisplayUtils.dpToPx(context, 1).toFloat()

        borderPaint = Paint().also {
            it.setAntiAlias(true)
            it.setStyle(Paint.Style.STROKE)
            it.setColor(Color.RED)
            it.setStrokeWidth(strokeWidth)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(padding, padding, width - padding, height - padding, borderPaint)
    }
}
