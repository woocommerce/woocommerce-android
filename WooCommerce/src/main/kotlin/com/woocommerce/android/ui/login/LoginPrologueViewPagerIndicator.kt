package com.woocommerce.android.ui.login

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import com.woocommerce.android.R

/**
 * Simple list of oval images used below the prologue view pager to indicate which page is selected
 */
class LoginPrologueViewPagerIndicator : LinearLayout {
    interface OnIndicatorClickedListener {
        fun onIndicatorClicked(index: Int)
    }

    private val indicators = ArrayList<ImageView>()
    private var listener: OnIndicatorClickedListener? = null

    init {
        orientation = HORIZONTAL
        val padding = context.resources.getDimensionPixelSize(R.dimen.margin_small)

        for (i in 0 until LoginPrologueViewPager.NUM_PAGES) {
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

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    fun setSelectedIndicator(index: Int) {
        for (i in 0 until LoginPrologueViewPager.NUM_PAGES) {
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
