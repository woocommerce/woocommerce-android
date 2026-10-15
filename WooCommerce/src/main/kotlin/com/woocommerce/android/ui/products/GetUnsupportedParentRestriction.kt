package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import javax.inject.Inject

class GetUnsupportedParentRestriction @Inject constructor(
    private val productDetailRepository: ProductDetailRepository,
    private val productRestrictions: OrderCreationProductRestrictions
) {
    suspend operator fun invoke(variation: Product): ProductRestriction.Unsupported? {
        val parent = productDetailRepository.getProduct(variation.parentId)
            ?: productDetailRepository.fetchAndGetProduct(variation.parentId)
        return parent?.let { productRestrictions.getUnsupportedRestriction(it) }
    }
}
