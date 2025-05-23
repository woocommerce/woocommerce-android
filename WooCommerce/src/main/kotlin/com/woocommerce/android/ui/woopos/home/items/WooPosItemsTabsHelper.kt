package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.CouponTab
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.HighlightLevel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.ProductTab
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.VariationTab
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WooPosItemsTabsHelper @Inject constructor(val resourceProvider: ResourceProvider) {
    val defaultTabs: List<Tab> =
        listOf(
            ProductTab(
                name = resourceProvider.getString(R.string.woopos_products_screen_title),
                highlightLevel = HighlightLevel.Full
            ),
            CouponTab(
                name = resourceProvider.getString(R.string.woopos_coupons_screen_title),
                highlightLevel = HighlightLevel.Normal
            ),
        )

    fun selectTab(
        tabs: List<Tab>,
        tab: Tab
    ): List<Tab> = tabs.map {
        if (it == tab) {
            it.copy(highlightLevel = HighlightLevel.Full)
        } else {
            it.copy(highlightLevel = HighlightLevel.Normal)
        }
    }

    private fun Tab.copy(
        name: String = this.name,
        highlightLevel: HighlightLevel = this.highlightLevel,
    ): Tab {
        return when (this) {
            is ProductTab -> ProductTab(name, highlightLevel)
            is CouponTab -> CouponTab(name, highlightLevel)
            is VariationTab -> VariationTab(name, highlightLevel)
        }
    }
}
