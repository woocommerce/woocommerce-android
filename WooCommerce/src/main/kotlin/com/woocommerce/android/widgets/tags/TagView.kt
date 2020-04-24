package com.woocommerce.android.widgets.tags

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
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
class TagView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.tagViewStyle,
    defStyleRes: Int = R.style.Woo_TextView_Caption
) : MaterialTextView(ctx, attrs, defStyleAttr, defStyleRes) {
    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TagView, defStyleAttr, defStyleRes)
            val config = TagConfig(context)
            try {
                config.tagText = a.getString(R.styleable.TagView_tagText).orEmpty()

                var textColor = a.getColor(R.styleable.TagView_tagTextColor, 0)
                if (textColor == 0) {
                    val textColorResId = a.getResourceId(R.styleable.TagView_tagTextColor, R.color.tagView_text)
                    textColor = ContextCompat.getColor(context, textColorResId)
                }
                config.fgColor = textColor

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
        setTextColor(config.fgColor)
        val gd = GradientDrawable()
        gd.setColor(config.bgColor)
        gd.cornerRadius = getDensityPixel(context, 4).toFloat()
        gd.setStroke(2, config.borderColor)
        background = gd
    }
}
