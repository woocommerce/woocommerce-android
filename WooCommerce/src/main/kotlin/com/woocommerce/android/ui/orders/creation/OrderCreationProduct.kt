package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfiguration
import com.woocommerce.android.ui.orders.creation.configuration.ProductRules
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
sealed class OrderCreationProduct(
    open val item: Order.Item,
    open val productInfo: ProductInfo
) : Parcelable {
    abstract fun isConfigurable(): Boolean
    abstract fun needsConfiguration(): Boolean

    abstract fun copyProduct(
        item: Order.Item = this.item,
        productInfo: ProductInfo = this.productInfo
    ): OrderCreationProduct

    data class ProductItem(
        override val item: Order.Item,
        override val productInfo: ProductInfo
    ) : OrderCreationProduct(item, productInfo) {
        override fun isConfigurable(): Boolean = false
        override fun needsConfiguration() = false
        override fun copyProduct(
            item: Order.Item,
            productInfo: ProductInfo
        ) = copy(item = item, productInfo = productInfo)
    }

    data class GroupedProductItem(
        override val item: Order.Item,
        override val productInfo: ProductInfo,
        val children: List<ProductItem>
    ) : OrderCreationProduct(item, productInfo) {
        override fun isConfigurable(): Boolean = false
        override fun needsConfiguration() = false
        override fun copyProduct(
            item: Order.Item,
            productInfo: ProductInfo
        ) = copy(item = item, productInfo = productInfo)
    }

    data class ProductItemWithRules(
        override val item: Order.Item,
        override val productInfo: ProductInfo,
        val rules: ProductRules,
        var configuration: ProductConfiguration = ProductConfiguration.getConfiguration(rules)
    ) : OrderCreationProduct(item, productInfo) {
        override fun isConfigurable(): Boolean = rules.isConfigurable()
        override fun needsConfiguration() = configuration.needsConfiguration()
        override fun copyProduct(
            item: Order.Item,
            productInfo: ProductInfo
        ) = copy(item = item, productInfo = productInfo)
    }

    data class GroupedProductItemWithRules(
        override val item: Order.Item,
        override val productInfo: ProductInfo,
        val children: List<ProductItem>,
        val rules: ProductRules,
        var configuration: ProductConfiguration = ProductConfiguration.getConfiguration(rules, children)
    ) : OrderCreationProduct(item, productInfo) {
        override fun isConfigurable(): Boolean = rules.isConfigurable()
        override fun needsConfiguration() = configuration.needsConfiguration()
        override fun copyProduct(
            item: Order.Item,
            productInfo: ProductInfo
        ) = copy(item = item, productInfo = productInfo)
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
    private val getProductRules: GetProductRules
) {
    suspend fun toOrderProducts(items: List<Order.Item>): List<OrderCreationProduct> {
        if (items.isEmpty()) return emptyList()

        return withContext(dispatchers.io) {
            val itemsMap = items.associateBy { item -> item.itemId }
            val childrenMap = mutableMapOf<Long, MutableList<OrderCreationProduct.ProductItem>>()
            val rulesMap = mutableMapOf<Long, ProductRules?>()

            val result = items.mapNotNull { item ->
                if ((item.productId in rulesMap.keys).not()) {
                    rulesMap[item.productId] = getProductRules.getRules(item)
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
        rules: ProductRules? = null,
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
}
