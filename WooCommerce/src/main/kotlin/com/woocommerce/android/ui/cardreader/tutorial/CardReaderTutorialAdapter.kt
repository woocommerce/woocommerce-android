package com.woocommerce.android.ui.cardreader.tutorial

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.woocommerce.android.R

class CardReaderTutorialAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    companion object {
        const val NUM_PAGES = 3
    }

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
