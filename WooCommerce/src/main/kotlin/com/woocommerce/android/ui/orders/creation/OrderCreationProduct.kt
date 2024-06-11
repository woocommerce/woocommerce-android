package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.configuration.GetProductConfiguration
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfiguration
import com.woocommerce.android.ui.orders.creation.configuration.ProductRules
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@Parcelize
sealed class OrderCreationProduct(
    open val item: Order.Item,
    open val productInfo: ProductInfo
) : Parcelable {
    abstract fun copyProduct(
        item: Order.Item = this.item,
        productInfo: ProductInfo = this.productInfo
    ): OrderCreationProduct

    abstract fun getConfiguration(): ProductConfiguration?

    data class ProductItem(
        override val item: Order.Item,
        override val productInfo: ProductInfo
    ) : OrderCreationProduct(item, productInfo) {
        override fun copyProduct(
            item: Order.Item,
            productInfo: ProductInfo
        ) = copy(item = item, productInfo = productInfo)

        override fun getConfiguration() = null
    }

    data class GroupedProductItem(
        override val item: Order.Item,
        override val productInfo: ProductInfo,
        val children: List<ProductItem>
    ) : OrderCreationProduct(item, productInfo) {
        override fun copyProduct(
            item: Order.Item,
            productInfo: ProductInfo
        ) = copy(item = item, productInfo = productInfo)

        override fun getConfiguration() = null
    }

    data class ProductItemWithRules(
        override val item: Order.Item,
        override val productInfo: ProductInfo,
        val rules: ProductRules,
        private var configuration: ProductConfiguration
    ) : OrderCreationProduct(item, productInfo) {
        override fun copyProduct(
            item: Order.Item,
            productInfo: ProductInfo
        ) = copy(item = item, productInfo = productInfo)

        override fun getConfiguration() = configuration
    }

    data class GroupedProductItemWithRules(
        override val item: Order.Item,
        override val productInfo: ProductInfo,
        val children: List<ProductItem>,
        val rules: ProductRules,
        private var configuration: ProductConfiguration
    ) : OrderCreationProduct(item, productInfo) {
        override fun copyProduct(
            item: Order.Item,
            productInfo: ProductInfo
        ) = copy(item = item, productInfo = productInfo)

        override fun getConfiguration() = configuration
    }
}

@Parcelize
data class ProductInfo(
    val imageUrl: String,
    val isStockManaged: Boolean,
    val stockQuantity: Double,
    val stockStatus: ProductStockStatus,
    val productType: ProductType,
    val isConfigurable: Boolean,
    val pricePreDiscount: String,
    val priceTotal: String,
    val priceSubtotal: String,
    val discountAmount: String,
    val priceAfterDiscount: String,
    val hasDiscount: Boolean,
) : Parcelable

class OrderCreationProductMapper @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val variationDetailRepository: VariationDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val getProductRules: GetProductRules,
    private val currencyFormatter: CurrencyFormatter,
    private val getProductConfiguration: GetProductConfiguration
) {
    suspend fun toOrderProducts(items: List<Order.Item>, currencySymbol: String? = null): List<OrderCreationProduct> {
        if (items.isEmpty()) return emptyList()

        return withContext(dispatchers.io) {
            val itemsMap = items.associateBy { item -> item.itemId }
            val childrenMap = mutableMapOf<Long, MutableList<OrderCreationProduct.ProductItem>>()
            val rulesMap = mutableMapOf<Long, ProductRules?>()

            val result = items.mapNotNull { item ->
                if ((item.productId in rulesMap.keys).not()) {
                    val rules = getProductRules.getRules(item)
                    rulesMap[item.productId] = rules
                }
                if (item.parent == null) {
                    item
                } else {
                    val children = childrenMap.getOrPut(item.parent) { mutableListOf() }
                    val productInfo = getProductInformation(item, currencySymbol)
                    children.add(OrderCreationProduct.ProductItem(item, productInfo))
                    null
                }
            }.filter { item ->
                (item.itemId in childrenMap.keys).not()
            }.map { item ->
                val productInfo = getProductInformation(item, currencySymbol)
                createOrderCreationProduct(item, productInfo, rulesMap[item.productId])
            }
                .toMutableList()

            for (parentId in childrenMap.keys) {
                val parent = itemsMap[parentId] ?: continue
                val children = childrenMap[parentId] ?: emptyList()
                val productInfo = getProductInformation(parent, currencySymbol)
                val rules = rulesMap[parent.productId]
                val groupedProduct = createOrderCreationProduct(parent, productInfo, rules, children)
                result.add(groupedProduct)
            }
            result
        }
    }

    private suspend fun createOrderCreationProduct(
        item: Order.Item,
        productInfo: ProductInfo,
        rules: ProductRules? = null,
        children: List<OrderCreationProduct.ProductItem>? = null
    ): OrderCreationProduct {
        return when {
            rules != null && children != null -> {
                val configuration = getProductConfiguration(
                    rules = rules,
                    children = children,
                    parentQuantity = item.quantity
                )
                OrderCreationProduct.GroupedProductItemWithRules(
                    item,
                    productInfo,
                    children,
                    rules,
                    configuration
                )
            }

            children != null -> OrderCreationProduct.GroupedProductItem(item, productInfo, children)
            rules != null -> {
                val configuration = getProductConfiguration(rules = rules)
                OrderCreationProduct.ProductItemWithRules(item, productInfo, rules, configuration)
            }
            else -> OrderCreationProduct.ProductItem(item, productInfo)
        }
    }

    private suspend fun getProductInformation(item: Order.Item, currencySymbol: String?): ProductInfo {
        return withContext(dispatchers.io) {
            val decimalFormatter = getDecimalFormatter(currencyFormatter, currencySymbol)
            if (item.isVariation) {
                val variation = variationDetailRepository.getVariation(item.productId, item.variationId)
                ProductInfo(
                    imageUrl = variation?.image?.source.orEmpty(),
                    isStockManaged = variation?.isStockManaged ?: false,
                    stockQuantity = variation?.stockQuantity ?: 0.0,
                    stockStatus = variation?.stockStatus ?: ProductStockStatus.InStock,
                    productType = ProductType.VARIATION,
                    isConfigurable = false,
                    pricePreDiscount = decimalFormatter(item.pricePreDiscount),
                    priceTotal = decimalFormatter(item.total),
                    priceSubtotal = decimalFormatter(item.subtotal),
                    discountAmount = decimalFormatter(item.discount),
                    priceAfterDiscount = decimalFormatter(item.subtotal - item.discount),
                    hasDiscount = item.discount > BigDecimal.ZERO
                )
            } else {
                val product = productDetailRepository.getProduct(item.productId)
                ProductInfo(
                    imageUrl = product?.firstImageUrl.orEmpty(),
                    isStockManaged = product?.isStockManaged ?: false,
                    stockQuantity = product?.stockQuantity ?: 0.0,
                    stockStatus = product?.specialStockStatus ?: product?.stockStatus ?: ProductStockStatus.InStock,
                    productType = product?.productType ?: ProductType.OTHER,
                    isConfigurable = product?.isConfigurable ?: false,
                    pricePreDiscount = decimalFormatter(item.pricePreDiscount),
                    priceTotal = decimalFormatter(item.total),
                    priceSubtotal = decimalFormatter(item.subtotal),
                    discountAmount = decimalFormatter(item.discount),
                    priceAfterDiscount = decimalFormatter(item.subtotal - item.discount),
                    hasDiscount = item.discount > BigDecimal.ZERO
                )
            }
        }
    }

    private fun getDecimalFormatter(
        currencyFormatter: CurrencyFormatter,
        currencyCode: String? = null
    ): (BigDecimal) -> String {
        return currencyCode?.let {
            currencyFormatter.buildBigDecimalFormatter(it)
        } ?: currencyFormatter.buildBigDecimalFormatter()
    }
}
