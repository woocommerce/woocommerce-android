package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import javax.inject.Inject

class WooPosItemsTabsHelper @Inject constructor() {
    val defaultTabs: List<WooPosItemsToolbarViewState.Tab> =
        listOf(
            WooPosItemsToolbarViewState.Tab(
                stringId = R.string.woopos_products_screen_title,
                highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
            ),
            WooPosItemsToolbarViewState.Tab(
                stringId = R.string.woopos_coupons_screen_title,
                highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
            ),
        )

    fun selectTab(tabs: List<WooPosItemsToolbarViewState.Tab>, tab: WooPosItemsToolbarViewState.Tab): List<WooPosItemsToolbarViewState.Tab> =
        tabs.map {
            if (it == tab) {
                it.copy(highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full)
            } else {
                it.copy(highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal)
            }
        }
}
