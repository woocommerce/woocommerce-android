package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant

sealed class WooPosItemNavigationData(open val id: Long) {
    data class VariableProductData(
        override val id: Long,
        val name: String,
        val numOfVariations: Int,
        val sourceType: WooPosAnalyticsEventConstant.ItemsListSourceType,
    ) : WooPosItemNavigationData(id)
}
