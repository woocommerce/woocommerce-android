package com.woocommerce.android.ui.products

interface BaseProductFragmentInterface {
    /**
     * Descendants can override this and return false to prevent the product from being
     * updated when the fragment is shown. This is useful for screens such as pickers.
     */
    var shouldUpdateProductWhenEntering: Boolean
}
