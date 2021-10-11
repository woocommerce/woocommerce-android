package com.woocommerce.android.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.woocommerce.android.R
import com.woocommerce.android.extensions.getColorCompat
import org.wordpress.android.util.DisplayUtils.dpToPx
import java.lang.Math.toDegrees
import kotlin.math.acos
import kotlin.math.asin

private const val PERCENTS_100 = 100.0
private const val PERCENTS_50 = 50.0
private const val ANGLE_180 = 180
private const val ANGLE_90 = 90
private const val ANIMATION_DURATION = 400L

class CircleProgressOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    private var progressAngle: Float? = null

    var currentProgressPercentage: Int = 0
        set(value) {
            if (measuredWidth <= 0) return
            val newAngle = calculateProgressAngle(value)
            animateProgress(progressAngle ?: ANGLE_90.toFloat(), newAngle)
            invalidate()
        }

    private var progressColor: Int = context.getColorCompat(R.color.color_on_primary)
    private var restColor: Int = context.getColorCompat(R.color.color_on_secondary)
    private var borderColor: Int = context.getColorCompat(R.color.color_accent_1)
    private var borderSizePx: Int = dpToPx(context, 2)

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

    private val animationInterpolator by lazy { LinearInterpolator() }

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
                borderSizePx = attrArray.getDimensionPixelSize(
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
            (width.toFloat() - borderSizePx * 2) / 2,
            restPaint
        )

        // progress
        progressAngle?.let {
            canvas.drawArc(
                borderSizePx.toFloat(),
                borderSizePx.toFloat(),
                width.toFloat() - borderSizePx,
                height.toFloat() - borderSizePx,
                it,
                ANGLE_180 - (it * 2),
                false,
                progressPaint
            )
        }
    }

    private fun animateProgress(from: Float, to: Float) {
        ValueAnimator.ofFloat(from, to).apply {
            duration = ANIMATION_DURATION
            interpolator = animationInterpolator
            addUpdateListener { valueAnimator ->
                progressAngle = valueAnimator.animatedValue as Float
                invalidate()
            }
        }.start()
    }

    private fun calculateProgressAngle(progressPercent: Int): Float {
        val wholeProgressHeight = height.toFloat() - borderSizePx * 2
        val radius = wholeProgressHeight / 2
        return if (progressPercent <= PERCENTS_50) {
            val cos = (radius - (radius * progressPercent * 2 / PERCENTS_100)) / radius
            ANGLE_90 - toDegrees(acos(cos)).toFloat()
        } else {
            val sin = (radius * (progressPercent - PERCENTS_50) * 2 / PERCENTS_100) / radius
            -toDegrees(asin(sin)).toFloat()
        }
    }
}
