package com.woocommerce.android.ui.promobanner

import androidx.annotation.StringRes
import com.woocommerce.android.R

enum class PromoBannerType(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int
) {
    LINKED_PRODUCTS(
        R.string.promo_linked_products_banner_title,
        R.string.promo_linked_products_banner_message
    )
}
