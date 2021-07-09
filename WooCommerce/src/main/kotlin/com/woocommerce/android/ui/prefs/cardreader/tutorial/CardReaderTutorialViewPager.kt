package com.woocommerce.android.ui.prefs.cardreader.tutorial

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCViewPagerIndicator

class CardReaderTutorialViewPager : LinearLayout {
    companion object {
        const val NUM_PAGES = 3
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    fun initViewPager(fragment: Fragment) {
        val viewPager = ViewPager2(context)
        addView(viewPager)
        viewPager.adapter = ViewPagerAdapter(fragment)

        val indicator = WCViewPagerIndicator(context)
        addView(indicator)
        indicator.setupFromViewPager(viewPager)
    }

    private inner class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        // note that the "sleep" step is skipped for now, since we don't support reconnection yet
        private val drawableIds = arrayOf(
            R.drawable.img_wc_ship_payment,
            R.drawable.img_wc_ship_card_insert,
            // R.drawable.img_wc_ship_card_sleep,
            R.drawable.img_wc_ship_card_charge
        )

        private val labelIds = arrayOf(
            R.string.card_reader_tutorial_connected_label,
            R.string.card_reader_tutorial_collect_label,
            // R.string.card_reader_tutorial_reconnect_label,
            R.string.card_reader_tutorial_charged_label
        )

        private val detailIds = arrayOf(
            R.string.card_reader_tutorial_connected_detail,
            R.string.card_reader_tutorial_collect_detail,
            // R.string.card_reader_tutorial_reconnect_detail,
            R.string.card_reader_tutorial_charged_detail
        )

        override fun createFragment(position: Int): Fragment {
            return CardReaderTutorialViewPagerItemFragment.newInstance(
                drawableIds[position],
                labelIds[position],
                detailIds[position]
            )
        }

        override fun getItemCount() = NUM_PAGES
    }
}
