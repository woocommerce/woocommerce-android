package com.woocommerce.android.ui.prefs.cardreader.tutorial

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCViewPager

class CardReaderTutorialViewPager : WCViewPager {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    fun initViewPager(fm: FragmentManager) {
        adapter = ViewPagerAdapter(fm)
    }

    private inner class ViewPagerAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm) {
        private val drawableIds = arrayOf(
            R.drawable.img_wc_ship_payment,
            R.drawable.img_wc_ship_card_insert,
            R.drawable.img_wc_ship_card_sleep,
            R.drawable.img_wc_ship_card_charge
        )

        private val labelIds = arrayOf(
            R.string.card_reader_tutorial_connected_label,
            R.string.card_reader_tutorial_collect_label,
            R.string.card_reader_tutorial_reconnect_label,
            R.string.card_reader_tutorial_charged_label
        )

        private val detailIds = arrayOf(
            R.string.card_reader_tutorial_connected_detail,
            R.string.card_reader_tutorial_collect_detail,
            R.string.card_reader_tutorial_reconnect_detail,
            R.string.card_reader_tutorial_charged_detail
        )

        override fun getItem(position: Int): Fragment {
            return CardReaderTutorialViewPagerItemFragment.newInstance(
                drawableIds[position],
                labelIds[position],
                detailIds[position]
            )
        }

        override fun getCount() = resources.getInteger(R.integer.card_reader_tutorial_page_count)
    }
}
