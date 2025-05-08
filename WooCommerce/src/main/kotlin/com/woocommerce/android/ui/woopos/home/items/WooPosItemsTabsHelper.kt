package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsCouponsFeatureFlagEnabled
import javax.inject.Inject

class WooPosItemsTabsHelper @Inject constructor(isCouponsFFEnabled: WooPosIsCouponsFeatureFlagEnabled) {
    val defaultTabs: List<WooPosItemsViewState.Tab> =
        listOf(
            WooPosItemsViewState.Tab(
                stringId = R.string.woopos_products_screen_title,
                highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Full
            ),
        ) + if (isCouponsFFEnabled()) {
            listOf(
                WooPosItemsViewState.Tab(
                    stringId = R.string.woopos_coupons_screen_title,
                    highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Normal
                )
            )
        } else {
            emptyList()
        }

    fun selectTab(tabs: List<WooPosItemsViewState.Tab>, tab: WooPosItemsViewState.Tab): List<WooPosItemsViewState.Tab> =
        tabs.map {
            if (it == tab) {
                it.copy(highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Full)
            } else {
                it.copy(highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Normal)
            }
        }
}
