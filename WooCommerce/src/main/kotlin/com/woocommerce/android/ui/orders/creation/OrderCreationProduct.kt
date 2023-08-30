package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.bundle.OrderItemRules
import com.woocommerce.android.ui.products.GetBundledProducts
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
sealed class OrderCreationProduct(
    open val item: Order.Item,
    open val productInfo: ProductInfo
) : Parcelable {

    abstract fun needsConfiguration(): Boolean
    data class ProductItem(
        override val item: Order.Item,
        override val productInfo: ProductInfo
    ) : OrderCreationProduct(item, productInfo) {
        override fun needsConfiguration() = false
    }

    data class GroupedProductItem(
        override val item: Order.Item,
        override val productInfo: ProductInfo,
        val children: List<ProductItem>
    ) : OrderCreationProduct(item, productInfo) {
        override fun needsConfiguration() = false
    }

    data class ProductItemWithRules(
        override val item: Order.Item,
        override val productInfo: ProductInfo,
        val rules: OrderItemRules
    ) : OrderCreationProduct(item, productInfo) {
        override fun needsConfiguration() = rules.needsConfiguration()
    }

    data class GroupedProductItemWithRules(
        override val item: Order.Item,
        override val productInfo: ProductInfo,
        val children: List<ProductItem>,
        val rules: OrderItemRules
    ) : OrderCreationProduct(item, productInfo) {
        override fun needsConfiguration() = rules.needsConfiguration()
    }
}

@Parcelize
data class ProductInfo(
    val imageUrl: String,
    val isStockManaged: Boolean,
    val stockQuantity: Double,
    val stockStatus: ProductStockStatus
) : Parcelable

class OrderCreationProductMapper @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val variationDetailRepository: VariationDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val getBundledProducts: GetBundledProducts
) {
    suspend fun toOrderProducts(items: List<Order.Item>): List<OrderCreationProduct> {
        if (items.isEmpty()) return emptyList()

        return withContext(dispatchers.io) {
            val itemsMap = items.associateBy { item -> item.itemId }
            val childrenMap = mutableMapOf<Long, MutableList<OrderCreationProduct.ProductItem>>()
            val rulesMap = mutableMapOf<Long, OrderItemRules?>()

            val result = items.mapNotNull { item ->
                if ((item.productId in rulesMap.keys).not()) {
                    rulesMap[item.productId] = getItemRules(item)
                }
                if (item.parent == null) {
                    item
                } else {
                    val children = childrenMap.getOrPut(item.parent) { mutableListOf() }
                    val productInfo = getProductInformation(item)
                    children.add(OrderCreationProduct.ProductItem(item, productInfo))
                    null
                }
            }.filter { item ->
                (item.itemId in childrenMap.keys).not()
            }.map { item ->
                val productInfo = getProductInformation(item)
                createOrderCreationProduct(item, productInfo, rulesMap[item.productId])
            }
                .toMutableList()

            for (parentId in childrenMap.keys) {
                val parent = itemsMap[parentId] ?: continue
                val children = childrenMap[parentId] ?: emptyList()
                val productInfo = getProductInformation(parent)
                val rules = rulesMap[parent.productId]
                val groupedProduct = createOrderCreationProduct(parent, productInfo, rules, children)
                result.add(groupedProduct)
            }
            result
        }
    }

    private fun createOrderCreationProduct(
        item: Order.Item,
        productInfo: ProductInfo,
        rules: OrderItemRules? = null,
        children: List<OrderCreationProduct.ProductItem>? = null
    ): OrderCreationProduct {
        return when {
            rules != null && children != null -> OrderCreationProduct.GroupedProductItemWithRules(
                item,
                productInfo,
                children,
                rules
            )

            children != null -> OrderCreationProduct.GroupedProductItem(item, productInfo, children)
            rules != null -> OrderCreationProduct.ProductItemWithRules(item, productInfo, rules)
            else -> OrderCreationProduct.ProductItem(item, productInfo)
        }
    }

    private suspend fun getProductInformation(item: Order.Item): ProductInfo {
        return withContext(dispatchers.io) {
            if (item.isVariation) {
                val variation = variationDetailRepository.getVariation(item.productId, item.variationId)
                ProductInfo(
                    variation?.image?.source.orEmpty(),
                    variation?.isStockManaged ?: false,
                    variation?.stockQuantity ?: 0.0,
                    variation?.stockStatus ?: ProductStockStatus.InStock
                )
            } else {
                val product = productDetailRepository.getProduct(item.productId)
                ProductInfo(
                    product?.firstImageUrl.orEmpty(),
                    product?.isStockManaged ?: false,
                    product?.stockQuantity ?: 0.0,
                    product?.specialStockStatus ?: product?.stockStatus ?: ProductStockStatus.InStock
                )
            }
        }
    }

    private suspend fun getItemRules(item: Order.Item): OrderItemRules? {
        if (item.isVariation) return null
        val product = productDetailRepository.getProduct(item.productId)
        val isBundle = product?.productType == ProductType.BUNDLE
        return if (isBundle) {
            val builder = OrderItemRules.Builder()
            getBundledProducts(item.productId).first().forEach { bundledProduct ->
                builder.setChildItemRule(
                    itemId = bundledProduct.id,
                    productId = bundledProduct.bundledProductId,
                    quantityMin = bundledProduct.rules.quantityMin,
                    quantityMax = bundledProduct.rules.quantityMax,
                    quantityDefault = bundledProduct.rules.quantityDefault,
                    optional = bundledProduct.rules.isOptional
                )
            }
            builder.build()
        } else {
            null
        }
    }
}
