package com.woocommerce.android.widgets.tags

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.util.TypedValue
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
 * @attr ref com.woocommerce.android.R.styleable#tagBorderColor
 */
class TagView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : AppCompatTextView(ctx, attrs, R.attr.tagViewStyle) {
    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TagView, R.attr.tagViewStyle, 0)
            val config = TagConfig(context)
            try {
                config.tagText = a.getString(R.styleable.TagView_tagText).orEmpty()

                var textColor = a.getColor(R.styleable.TagView_tagTextColor, 0)
                if (textColor == 0) {
                    val textColorResId = a.getResourceId(R.styleable.TagView_tagTextColor, R.color.tagView_text)
                    textColor = ContextCompat.getColor(context, textColorResId)
                }
                config.fgColor = textColor

                val textSize = a.getDimensionPixelSize(R.styleable.TagView_tagTextSize, 0)
                if (textSize == 0) {
                    config.textSize = context.resources.getDimension(R.dimen.tag_text_size)
                } else {
                    config.textSize = textSize.toFloat()
                }

                var bgColor = a.getColor(R.styleable.TagView_tagColor, 0)
                if (bgColor == 0) {
                    val bgColorResId = a.getResourceId(R.styleable.TagView_tagColor, R.color.tagView_bg)
                    bgColor = ContextCompat.getColor(context, bgColorResId)
                }
                config.bgColor = bgColor

                var borderColor = a.getColor(R.styleable.TagView_tagBorderColor, 0)
                if (borderColor == 0) {
                    val borderColorResId =
                            a.getResourceId(R.styleable.TagView_tagBorderColor, R.color.tagView_border_bg)
                    borderColor = ContextCompat.getColor(context, borderColorResId)
                }
                config.borderColor = borderColor

                initTag(config)
            } finally {
                a.recycle()
            }
        }
    }

    var tag: ITag? = null
    set(v) {
        field = v
        tag?.let {
            initTag(it.getTagConfiguration(context))
        }
    }

    /**
     * Should be called anytime the tag changes. Sets the background color, border
     * color and corner radius of the tag view.
     */
    private fun initTag(config: TagConfig) {
        text = config.tagText
        setTextSize(TypedValue.COMPLEX_UNIT_PX, config.textSize)
        setTextColor(config.fgColor)
        val gd = GradientDrawable()
        gd.setColor(config.bgColor)
        gd.cornerRadius = getDensityPixel(context, 2).toFloat()
            gd.setStroke(3, config.borderColor)
        background = gd
    }
}
