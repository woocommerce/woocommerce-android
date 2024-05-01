package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.orders.creation.configuration.GetProductConfiguration
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfiguration
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

class CreateOrderItem @Inject constructor(
    private val coroutineDispatchers: CoroutineDispatchers,
    private val variationDetailRepository: VariationDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val getProductRules: GetProductRules,
    private val getProductConfiguration: GetProductConfiguration
) {
    suspend operator fun invoke(
        remoteProductId: Long,
        variationId: Long? = null,
        productConfiguration: ProductConfiguration? = null,
    ): Order.Item {
        return withContext(coroutineDispatchers.io) {
            val product = productDetailRepository.fetchProductOrLoadFromCache(remoteProductId)
            // Try to get the default configuration for not configurable bundles
            val configuration = if (product?.productType == ProductType.BUNDLE && productConfiguration == null) {
                getProductRules.getRules(remoteProductId)?.let { getProductConfiguration(it) }
            } else {
                productConfiguration
            }

            variationId?.let {
                if (product != null) {
                    variationDetailRepository.getVariationOrNull(remoteProductId, it)
                        ?.createItem(product, configuration)
                } else {
                    null
                }
            } ?: product?.createItem(configuration)
                ?: Order.Item.EMPTY.copy(productId = remoteProductId, variationId = variationId ?: 0L)
        }
    }

    private fun Product.createItem(productConfiguration: ProductConfiguration?): Order.Item = Order.Item(
        itemId = 0L,
        productId = remoteId,
        variationId = 0L,
        quantity = 1f,
        name = name,
        price = price ?: BigDecimal.ZERO,
        subtotal = price ?: BigDecimal.ZERO,
        totalTax = BigDecimal.ZERO,
        total = price ?: BigDecimal.ZERO,
        sku = sku,
        attributesList = emptyList(),
        configuration = productConfiguration
    )

    private fun ProductVariation.createItem(
        parentProduct: Product,
        productConfiguration: ProductConfiguration?
    ): Order.Item = Order.Item(
        itemId = 0L,
        productId = remoteProductId,
        variationId = remoteVariationId,
        quantity = 1f,
        name = parentProduct.name,
        price = price ?: BigDecimal.ZERO,
        subtotal = price ?: BigDecimal.ZERO,
        totalTax = BigDecimal.ZERO,
        total = price ?: BigDecimal.ZERO,
        sku = sku,
        attributesList = attributes
            .filterNot { it.name.isNullOrEmpty() || it.option.isNullOrEmpty() }
            .map { Order.Item.Attribute(it.name!!, it.option!!) },
        configuration = productConfiguration
    )
}
