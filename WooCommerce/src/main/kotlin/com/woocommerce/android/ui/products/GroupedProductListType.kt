package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R

enum class GroupedProductListType(@StringRes val titleId: Int, val resultKey: String) {
    GROUPED(R.string.grouped_products, "key_grouped"),
    UPSELLS(R.string.upsells_label, "key_upsells"),
    CROSS_SELLS(R.string.cross_sells_label, "key_cross-sells");
}
