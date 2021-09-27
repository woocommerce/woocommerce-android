package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import org.wordpress.android.util.DisplayUtils.dpToPx
import java.lang.Math.toDegrees
import kotlin.math.asin

private const val PERCENTS_100 = 100.0
private const val ANGLE_180 = 180

class CircleProgressOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    private var progressAngle = 0f

    var currentProgressPercentage: Int = 0
        set(value) {
            progressAngle = calculateProgressAngle(value)
            invalidate()
            field = value
        }

    private var progressColor: Int = context.getColor(R.color.color_on_primary)
    private var restColor: Int = context.getColor(R.color.color_on_secondary)
    private var borderColor: Int = context.getColor(R.color.color_accent_1)
    private var borderSize: Int = dpToPx(context, 2)

    private val progressPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = progressColor
        }
    }
    private val restPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = restColor
        }
    }

    private val borderPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
        }
    }

    init {
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.CircleProgressOverlayView)
            try {
                progressColor = attrArray.getColor(
                    R.styleable.CircleProgressOverlayView_circleProgressProgressColor,
                    progressColor
                )
                restColor = attrArray.getColor(
                    R.styleable.CircleProgressOverlayView_circleProgressRestColor,
                    restColor
                )
                borderColor = attrArray.getColor(
                    R.styleable.CircleProgressOverlayView_circleProgressBorderColor,
                    borderColor
                )
                borderSize = attrArray.getDimensionPixelSize(
                    R.styleable.CircleProgressOverlayView_circleProgressBorderSize,
                    dpToPx(context, 2)
                )
            } finally {
                attrArray.recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        // border
        canvas.drawCircle(
            width.toFloat() / 2,
            height.toFloat() / 2,
            width.toFloat() / 2,
            borderPaint
        )

        // main
        canvas.drawCircle(
            width.toFloat() / 2,
            height.toFloat() / 2,
            (width.toFloat() - borderSize * 2) / 2,
            restPaint
        )

        // progress
        canvas.drawArc(
            borderSize.toFloat(),
            borderSize.toFloat(),
            width.toFloat() - borderSize,
            height.toFloat() - borderSize,
            progressAngle,
            ANGLE_180 - progressAngle,
            false,
            progressPaint
        )
        println("Start $progressAngle sweep ${ANGLE_180 - progressAngle}")
    }

    private fun calculateProgressAngle(progressPercent: Int): Float {
        val wholeProgressHeight = height.toFloat() - borderSize * 2
        val radius = wholeProgressHeight / 2
        val sin = (wholeProgressHeight * progressPercent / PERCENTS_100) / radius
        return 90 - toDegrees(asin(sin)).toFloat()
    }
}
