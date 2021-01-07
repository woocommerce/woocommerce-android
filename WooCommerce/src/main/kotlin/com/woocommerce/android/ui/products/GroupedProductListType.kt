package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.ConnectedProductsListContext

enum class GroupedProductListType(
    @StringRes val titleId: Int,
    val resultKey: String,
    val statContext: ConnectedProductsListContext
) {
    GROUPED(R.string.grouped_products, "key_grouped", ConnectedProductsListContext.GROUPED_PRODUCTS),
    UPSELLS(R.string.upsells_label, "key_upsells", ConnectedProductsListContext.UPSELLS),
    CROSS_SELLS(R.string.cross_sells_label, "key_cross-sells", ConnectedProductsListContext.CROSS_SELLS);
}
