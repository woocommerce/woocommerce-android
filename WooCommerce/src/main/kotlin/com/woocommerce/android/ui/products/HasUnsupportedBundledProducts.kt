package com.woocommerce.android.ui.products

import com.woocommerce.android.ui.products.details.ProductDetailRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Tells whether a bundle holds a product which can't be added to an order, by asking the same restrictions the
 * product list asks. The bundled products are not part of the cached product list, so this resolves them and is only
 * worth calling once the merchant picks the bundle.
 *
 * Only the [ProductRestriction.Unsupported] restrictions apply: a [ProductRestriction.Hidden] one merely keeps a
 * product out of the list — a bundled product being unpublished or having no price of its own says nothing about
 * whether the bundle can be sold.
 *
 * A bundled product which can't be resolved (its fetch failed and nothing is cached) leaves the answer [Result.UNKNOWN]
 * rather than letting the bundle through unchecked.
 */
class HasUnsupportedBundledProducts @Inject constructor(
    private val getBundledProducts: GetBundledProducts,
    private val productDetailRepository: ProductDetailRepository,
    private val productRestrictions: OrderCreationProductRestrictions
) {
    suspend operator fun invoke(productId: Long): Result {
        val bundledProducts = getBundledProducts(productId).first()
        val resolvedProducts = bundledProducts.mapNotNull { productDetailRepository.getProduct(it.bundledProductId) }
        return when {
            resolvedProducts.any { productRestrictions.getUnsupportedRestriction(it) != null } -> Result.YES
            resolvedProducts.size < bundledProducts.size -> Result.UNKNOWN
            else -> Result.NO
        }
    }

    enum class Result { NO, YES, UNKNOWN }
}
