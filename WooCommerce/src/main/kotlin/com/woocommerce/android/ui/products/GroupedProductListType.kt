package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R

enum class GroupedProductListType(@StringRes val titleId: Int) {
    GROUPED(R.string.grouped_products),
    UPSELLS(R.string.upsells_label),
    CROSS_SELLS(R.string.cross_sells_label);
}
