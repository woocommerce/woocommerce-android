package com.woocommerce.android.ui.woopos.home.items.variations

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant

data class WooPosVariableProductNavigationData(
    val id: Long,
    val name: String,
    val numOfVariations: Int,
    val sourceType: WooPosAnalyticsEventConstant.ItemsListSourceType,
)
