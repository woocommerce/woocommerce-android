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
    suspend operator fun invoke(productIds: List<Pair<Long, List<Long?>?>>): List<Order.Item> {
        return withContext(coroutineDispatchers.io) {
            val productsWithoutVariations = productIds.filter { productPair ->
                productPair.second == null
            }

            val productsWithVariations = productIds.filter { productPair ->
                productPair.second != null
            }

            val productsWithoutVariationsOrderItems = productsWithoutVariations.map { productPair ->
                val product = productDetailRepository.getProduct(productPair.first)
                product?.createItem()
                    ?: Order.Item.EMPTY.copy(productId = productPair.first, variationId = 0L)
            }

            var productsWithVariationsOrderItems = mutableListOf<Order.Item>()
            productsWithVariations.forEach { productPair ->
                val product = productDetailRepository.getProduct(productPair.first)
                productPair.second!!.let { variationIds ->
                    productsWithVariationsOrderItems.addAll(variationIds.map { variationId ->
                        variationId?.let {
                            if (product != null) {
                                variationDetailRepository.getVariation(productPair.first, it)
                                    ?.createItem(product)
                            } else null
                        } ?: product?.createItem()
                        ?: Order.Item.EMPTY.copy(productId = productPair.first, variationId = variationId ?: 0L)
                    }.toMutableList())
                }
            }
            productsWithVariationsOrderItems + productsWithoutVariationsOrderItems
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
