package com.woocommerce.android.ui.products

import com.woocommerce.android.ui.products.ProductDetailViewModel.CommonViewState

interface BaseProductFragmentView {
    /**
     * Inherited by child fragments to update Product model
     */
    fun updateProductView(productData: CommonViewState)

    /**
     * Inherited by child fragments to display/hide DONE menu button
     * depending on product fields that were edited
     */
    fun showUpdateProductAction(show: Boolean)
}
