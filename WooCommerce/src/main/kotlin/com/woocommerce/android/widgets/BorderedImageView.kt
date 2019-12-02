package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.woocommerce.android.R
import org.wordpress.android.util.DisplayUtils

/**
 * ImageView with a built-in border so we can avoid adding a parent ViewGroup to provide the border
 */
class BorderedImageView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : AppCompatImageView(
        ctx,
        attrs
) {
    private val borderPaint: Paint

    private var borderSize = context.resources.getDimensionPixelSize(R.dimen.image_border_size).toFloat()
    private var borderRadius = DisplayUtils.dpToPx(context, 2).toFloat()
    private var borderColor = ContextCompat.getColor(context, R.color.list_divider)

    init {
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.BorderedImageView)
            try {
                borderSize = attrArray.getDimension(R.styleable.BorderedImageView_borderSize, borderSize)
                borderRadius = attrArray.getFloat(R.styleable.BorderedImageView_borderRadius, borderRadius)
                borderColor = attrArray.getColor(R.styleable.BorderedImageView_borderColor, borderColor)
            } finally {
                attrArray.recycle()
            }
        }

        

        borderPaint = Paint().also { paint ->
            paint.isAntiAlias = true
            paint.style = Paint.Style.STROKE
            paint.color = borderColor
            paint.strokeWidth = borderSize
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(
                0f, // left
                0f, // top
                width.toFloat(), // right
                height.toFloat(), // bottom
                borderRadius,
                borderRadius,
                borderPaint
        )
    }
}
