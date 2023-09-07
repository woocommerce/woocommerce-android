package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.ui.products.ProductDetailRepository
import javax.inject.Inject

class AttributeTermsListHandler @Inject constructor(
    private val repository: ProductDetailRepository
) {
    companion object {
        private const val PAGE_SIZE = 20
    }
}
