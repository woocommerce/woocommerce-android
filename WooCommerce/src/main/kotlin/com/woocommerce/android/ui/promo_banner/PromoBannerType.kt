package com.woocommerce.android.ui.promo_banner

import androidx.annotation.StringRes
import com.woocommerce.android.R

enum class PromoBannerType(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int
) {
    LINKED_PRODUCTS(
        R.string.product_detail_linked_products,
        R.string.product_detail_linked_products
    )
}
