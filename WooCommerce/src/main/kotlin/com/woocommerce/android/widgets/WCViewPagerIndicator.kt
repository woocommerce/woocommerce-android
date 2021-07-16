package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
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
    private val indicators = ArrayList<ImageView>()
    private var pageCount: Int = 0

    fun setupFromViewPager(viewPager: ViewPager2) {
        pageCount = viewPager.adapter?.itemCount ?: 0

        orientation = HORIZONTAL
        val padding = context.resources.getDimensionPixelSize(R.dimen.margin_small)

        for (page in 0 until pageCount) {
            ImageView(context).also { imageView ->
                imageView.setImageResource(R.drawable.ic_tab_indicator)
                imageView.isSelected = page == 0
                imageView.setPadding(padding, 0, padding, 0)
                this.addView(imageView)
                indicators.add(imageView)

                imageView.setOnClickListener {
                    viewPager.setCurrentItem(page, true)
                }
            }
        }

        val listener = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setSelectedIndicator(position)
            }
        }
        viewPager.registerOnPageChangeCallback(listener)
    }

    private fun setSelectedIndicator(index: Int) {
        for (i in 0 until pageCount) {
            indicators[i].isSelected = i == index
        }
    }
}
