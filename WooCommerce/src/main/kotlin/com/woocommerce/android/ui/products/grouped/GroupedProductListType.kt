package com.woocommerce.android.ui.products.grouped

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker

enum class GroupedProductListType(
    @StringRes val titleId: Int,
    val resultKey: String,
    val statContext: AnalyticsTracker.Companion.ConnectedProductsListContext
) {
    GROUPED(
        R.string.grouped_products,
        "key_grouped",
        AnalyticsTracker.Companion.ConnectedProductsListContext.GROUPED_PRODUCTS
    ),
    UPSELLS(R.string.upsells_label, "key_upsells", AnalyticsTracker.Companion.ConnectedProductsListContext.UPSELLS),
    CROSS_SELLS(
        R.string.cross_sells_label,
        "key_cross-sells",
        AnalyticsTracker.Companion.ConnectedProductsListContext.CROSS_SELLS
    )
}
