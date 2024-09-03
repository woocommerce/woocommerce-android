package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.configuration.ProductRules
import com.woocommerce.android.ui.products.GetBundledProducts
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetProductRules @Inject constructor(
    private val productDetailRepository: ProductDetailRepository,
    private val getBundledProducts: GetBundledProducts
) {
    suspend fun getRules(item: Order.Item): ProductRules? {
        if (item.isVariation) return null
        return productDetailRepository.getProductFromLocalCache(item.productId)?.let { getRules(it) }
    }

    suspend fun getRules(productId: Long): ProductRules? {
        return productDetailRepository.getProductFromLocalCache(productId)?.let { getRules(it) }
    }

    private suspend fun getRules(product: Product): ProductRules? {
        val isBundle = product.productType == ProductType.BUNDLE
        return if (isBundle) {
            val builder = ProductRules.Builder().apply {
                productType = ProductType.BUNDLE
            }
            builder.setQuantityRules(quantityMin = product.bundleMinSize, quantityMax = product.bundleMaxSize)
            getBundledProducts(product.remoteId).first().forEach { bundledProduct ->
                builder.setChildQuantityRules(
                    itemId = bundledProduct.id,
                    quantityMin = bundledProduct.rules.quantityMin,
                    quantityMax = bundledProduct.rules.quantityMax,
                    quantityDefault = bundledProduct.rules.quantityDefault
                )
                if (bundledProduct.rules.isOptional) {
                    builder.setChildOptional(bundledProduct.id)
                }
                if (bundledProduct.isVariable) {
                    builder.setChildVariableRules(
                        bundledProduct.id,
                        bundledProduct.rules.attributesDefault,
                        bundledProduct.rules.variationIds
                    )
                }
            }
            builder.build()
        } else {
            null
        }
    }
}
