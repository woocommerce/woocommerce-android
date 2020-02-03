package com.woocommerce.android.ui.products

interface BaseProductFragmentView {
    /**
     * Inherited by child fragments to display/hide DONE menu button
     * depending on product fields that were edited
     */
    fun showUpdateProductAction(show: Boolean)
}
