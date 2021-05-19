package com.woocommerce.android.ui.login

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import com.woocommerce.android.R

/**
 * Simple list of images used below the prologue view pager to indicate with page is selected
 */
class LoginPrologueViewPagerIndicator : LinearLayout {
    companion object {
        private const val NUM_INDICATORS = 4
    }

    private val indicators = ArrayList<ImageView>()

    init {
        orientation = HORIZONTAL
        val margin = context.resources.getDimensionPixelSize(R.dimen.margin_small)

        for (index in 0 until NUM_INDICATORS) {
            ImageView(context).also { imageView ->
                imageView.setImageResource(R.drawable.ic_tab_indicator)
                imageView.isSelected = index == 0
                this.addView(imageView)
                indicators.add(imageView)

                (imageView.layoutParams as MarginLayoutParams).also {
                    it.marginEnd = margin
                    it.marginStart = margin
                }
            }
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    fun setSelectedIndicator(selectedIndex: Int) {
        for (index in 0 until NUM_INDICATORS) {
            indicators[index].isSelected = index == selectedIndex
        }
    }
}
