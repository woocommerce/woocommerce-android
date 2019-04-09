package com.woocommerce.android.ui.products

object ProductHelper {
    /**
     * Simple helper which returns the variation ID if it's not null an non-zero, otherwise returns the product ID
     * if it's not null (and if it is, returns 0). This is useful when deciding whether to use a product ID or a
     * variation ID when looking up a product - we want to favor the variation ID when available because that will
     * get us the actual variation
     */
    fun productOrVariationId(productId: Long?, variationId: Long?): Long {
        variationId?.let {
            if (it != 0L) {
                return it
            }
        }
        productId?.let {
            return it
        }
        return 0L
    }
}
