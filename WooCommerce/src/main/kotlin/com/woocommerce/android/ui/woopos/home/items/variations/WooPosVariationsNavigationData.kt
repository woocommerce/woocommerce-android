package com.woocommerce.android.ui.woopos.home.items.variations

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant

data class WooPosVariationsNavigationData(
    val id: Long,
    val numOfVariations: Int,
    val sourceType: WooPosAnalyticsEventConstant.ItemsListSourceType,
)
