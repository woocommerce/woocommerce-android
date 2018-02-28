package com.woocommerce.android.widgets.tags

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.util.getDensityPixel

/**
 * Custom tag view. This view displays a simple string label. The background and font colors are both styleable.
 * Font color will default to gray, background color will default to light gray.
 *
 * Has three custom xml attributes available:
 * * tagLabel: String to display in the tag
 * * tagFontColor: Font color
 * * tagBackgroundColor: Background color
 */
class TagView constructor(ctx: Context,
                          attrs: AttributeSet? = null) : FrameLayout(ctx) {
    var tag: ITag? = null
    set(v) {
        field = v
        textView.text = tag?.getFormattedLabel(context).orEmpty()

        val bg = tag?.bgColor ?: Color.LTGRAY
        initBackground(bg)

        val fg = tag?.fgColor ?: Color.GRAY
        textView.setTextColor(fg)
    }

    private val textView: TextView

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.tag_view, this)
        textView = getChildAt(0) as TextView

        attrs?.let {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TagView, 0, 0)
            try {
                val labelStr = a.getString(R.styleable.TagView_tagLabel)
                labelStr?.let { textView.text = labelStr }

                val fontColor = a.getColor(R.styleable.TagView_tagFontColor, Color.GRAY)
                textView.setTextColor(fontColor)

                val bgColor = a.getColor(R.styleable.TagView_tagBackgroundColor, Color.LTGRAY)
                initBackground(bgColor)
            } finally {
                a.recycle()
            }
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
        textView.background = gd
    }
}
