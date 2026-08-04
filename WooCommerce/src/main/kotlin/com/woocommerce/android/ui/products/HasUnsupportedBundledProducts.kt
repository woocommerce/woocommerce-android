package com.woocommerce.android.ui.products

import com.woocommerce.android.ui.products.details.ProductDetailRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Tells whether a bundle holds a product which can't be added to an order, by asking the same restrictions the
 * product list asks. The bundled products are not part of the cached product list, so this resolves them and is only
 * worth calling once the merchant picks the bundle.
 *
 * Only the restrictions carrying a reason apply: they are the ones saying a product can never be sold from the app.
 * The rest merely keep a product out of the list — a bundled product being unpublished or having no price of its own
 * says nothing about whether the bundle can be sold.
 */
class HasUnsupportedBundledProducts @Inject constructor(
    private val getBundledProducts: GetBundledProducts,
    private val productDetailRepository: ProductDetailRepository,
    private val productRestrictions: OrderCreationProductRestrictions
) {
    suspend operator fun invoke(productId: Long): Boolean {
        return getBundledProducts(productId).first().any { bundledProduct ->
            val product = productDetailRepository.getProduct(bundledProduct.bundledProductId)
            product != null && productRestrictions.getNonSelectableRestriction(product) != null
        }
    }
}
