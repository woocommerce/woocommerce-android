package com.woocommerce.android.widgets.tags

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.util.getDensityPixel

/**
 * Custom tag view. This view displays a simple string label. The background and font colors are both styleable.
 * Font color will default to gray, background color will default to light gray.
 *
 * @attr ref com.woocommerce.android.R.styleable#tagText
 * @attr ref com.woocommerce.android.R.styleable#tagTextColor
 * @attr ref com.woocommerce.android.R.styleable#tagTextSize
 * @attr ref com.woocommerce.android.R.styleable#tagColor
 */
class TagView @JvmOverloads constructor(ctx: Context,
                                        attrs: AttributeSet? = null) : TextView(ctx, attrs, R.attr.tagViewStyle) {
    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TagView, R.attr.tagViewStyle, 0)
        try {
            val labelStr = a.getString(R.styleable.TagView_tagText)
            labelStr?.let { text = labelStr }

            val textColor = a.getColor(R.styleable.TagView_tagTextColor, Color.GRAY)
            setTextColor(textColor)

            val textSize = a.getDimensionPixelSize(R.styleable.TagView_tagTextSize, 0)
            if (textSize > 0) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
            }

            val bgColor = a.getColor(R.styleable.TagView_tagColor, Color.LTGRAY)
            initBackground(bgColor)
        } finally {
            a.recycle()
        }
    }

    var tag: ITag? = null
    set(v) {
        field = v
        text = tag?.getFormattedLabel(context).orEmpty()

        tag?.let {
            setTextColor(tag!!.fgColor)
            initBackground(tag!!.bgColor)
        }
    }

    /**
     * Should be called anytime the tag changes. Sets the background color and
     * corner radius of the tag view.
     */
    private fun initBackground(baseColor: Int) {
        val gd = GradientDrawable()
        gd.setColor(baseColor)
        gd.cornerRadius = getDensityPixel(context, 2).toFloat()
        background = gd
    }
}
