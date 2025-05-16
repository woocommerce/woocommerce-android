package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import javax.inject.Inject

class WooPosItemsTabsHelper @Inject constructor() {
    val defaultTabs: List<WooPosItemsViewState.Tab> =
        listOf(
            WooPosItemsViewState.Tab(
                stringId = R.string.woopos_products_screen_title,
                highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Full
            ),
            WooPosItemsViewState.Tab(
                stringId = R.string.woopos_coupons_screen_title,
                highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Normal
            ),
        )

    fun selectTab(tabs: List<WooPosItemsViewState.Tab>, tab: WooPosItemsViewState.Tab): List<WooPosItemsViewState.Tab> =
        tabs.map {
            if (it == tab) {
                it.copy(highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Full)
            } else {
                it.copy(highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Normal)
            }
        }
}
