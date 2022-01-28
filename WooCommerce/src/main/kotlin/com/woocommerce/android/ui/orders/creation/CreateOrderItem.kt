package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

class CreateOrderItem @Inject constructor(
    private val coroutineDispatchers: CoroutineDispatchers,
    private val variationDetailRepository: VariationDetailRepository,
    private val productDetailRepository: ProductDetailRepository
) {
    suspend operator fun invoke(remoteProductId: Long, variationId: Long? = null): Order.Item {
        return withContext(coroutineDispatchers.io) {
            val product = productDetailRepository.getProduct(remoteProductId)

            variationId?.let {
                if (product != null) {
                    variationDetailRepository.getVariation(remoteProductId, it)?.createItem(product)
                } else null
            } ?: product?.createItem()
                ?: Order.Item.EMPTY.copy(productId = remoteProductId, variationId = variationId ?: 0L)
        }
    }

    private fun Product.createItem(): Order.Item = Order.Item(
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
    )

    private fun ProductVariation.createItem(parentProduct: Product): Order.Item = Order.Item(
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
            .map { Order.Item.Attribute(it.name!!, it.option!!) }
    )
}
