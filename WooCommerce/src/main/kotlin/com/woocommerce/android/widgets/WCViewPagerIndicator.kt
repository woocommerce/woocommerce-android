package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.use
import com.woocommerce.android.R

/**
 * Simple list of oval images used below a view pager to indicate which page is selected
 */
class WCViewPagerIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    interface OnIndicatorClickedListener {
        fun onIndicatorClicked(index: Int)
    }

    private val indicators = ArrayList<ImageView>()
    private var listener: OnIndicatorClickedListener? = null
    private var pageCount: Int = 0

    init {
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.WCViewPagerIndicator).use {
                pageCount = it.getInt(R.styleable.WCViewPagerIndicator_pageCount, 0)
            }
        }

        orientation = HORIZONTAL
        val padding = context.resources.getDimensionPixelSize(R.dimen.margin_small)

        for (i in 0 until pageCount) {
            ImageView(context).also { imageView ->
                imageView.setImageResource(R.drawable.ic_tab_indicator)
                imageView.isSelected = i == 0
                imageView.setPadding(padding, 0, padding, 0)
                this.addView(imageView)
                indicators.add(imageView)

                imageView.setOnClickListener {
                    itemClicked(i)
                }
            }
        }
    }

    fun setSelectedIndicator(index: Int) {
        for (i in 0 until pageCount) {
            indicators[i].isSelected = i == index
        }
    }

    fun setListener(listener: OnIndicatorClickedListener) {
        this.listener = listener
    }

    fun itemClicked(index: Int) {
        listener?.onIndicatorClicked(index)
    }
}
