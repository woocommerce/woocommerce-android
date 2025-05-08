package com.woocommerce.android.ui.woopos.home.cart

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState.Coupon.CouponValidationState
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLogWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WooPosCartItemsUpdater @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val resourceProvider: ResourceProvider,
    private val formatPrice: WooPosFormatPrice,
    private val productsCache: WooPosProductsCache,
    private val wooLogWrapper: WooLogWrapper,
    private val crashLogger: CrashLogging,
) {
    suspend operator fun invoke(
        itemsInCart: List<WooPosCartItemViewState>,
        updatedProducts: List<ParentToChildrenEvent.OrderCreated.ProductInfo>,
        updatedCoupons: List<ParentToChildrenEvent.OrderCreated.CouponInfo>,
    ): List<WooPosCartItemViewState> {
        val mutableCurrentBodyList = itemsInCart.toMutableList()
        var productsChanged = false

        val availableProductsMap = createAvailableProductsMap(updatedProducts)

        itemsInCart.forEachIndexed { index, item ->
            when (item) {
                is WooPosCartItemViewState.Product -> {
                    val productKey = getProductKey(item)
                    val availableQuantity = availableProductsMap[productKey] ?: 0

                    if (availableQuantity > 0) {
                        availableProductsMap[productKey] = availableQuantity - 1
                        val updatedProduct = findMatchingProduct(item, updatedProducts)

                        updatedProduct?.let {
                            val updatedItem = updateProductWithNewInfo(item, it)
                            val itemChanged = updatedItem.name != item.name || updatedItem.price != item.price

                            if (itemChanged) {
                                updateProductInCache(updatedItem, updatedProduct)
                            }

                            mutableCurrentBodyList[index] = updatedItem
                            productsChanged = productsChanged || itemChanged
                        }
                    } else {
                        val updatedItem = markProductAsNotExisting(item)
                        mutableCurrentBodyList[index] = updatedItem
                        val itemChanged = updatedItem.name != item.name ||
                            updatedItem.price != item.price ||
                            updatedItem.productDoesNotExist != item.productDoesNotExist

                        if (itemChanged) {
                            deleteProductFromCache(updatedItem.id)
                        }
                        productsChanged = productsChanged || itemChanged
                    }
                }

                is WooPosCartItemViewState.Coupon -> {
                    mutableCurrentBodyList[index] = updateCouponsWithFormattedDiscount(updatedCoupons, item)
                }
            }
        }

        if (productsChanged) {
            notifyParentAboutChanges()
        }

        return mutableCurrentBodyList
    }

    private suspend fun updateCouponsWithFormattedDiscount(
        updatedCoupons: List<ParentToChildrenEvent.OrderCreated.CouponInfo>,
        item: WooPosCartItemViewState.Coupon,
    ) = updatedCoupons.find { it.code == item.name }?.let {
        item.copy(validationState = CouponValidationState.Valid("-${formatPrice(it.discountAmount)}"))
    } ?: item.also {
        val message = "Coupon not found in the cart"
        wooLogWrapper.e(T.POS, message)
        crashLogger.sendReport(IllegalStateException(message))
    }

    private suspend fun notifyParentAboutChanges() {
        childrenToParentEventSender.sendToParent(
            ChildToParentEvent.ToastMessageDisplayed(
                message = resourceProvider.getString(R.string.woopos_cart_changes_in_the_cart)
            )
        )
    }

    private suspend fun updateProductInCache(
        updatedItem: WooPosCartItemViewState.Product,
        updatedProduct: ParentToChildrenEvent.OrderCreated.ProductInfo
    ) {
        productsCache.getProductById(updatedItem.id)?.let { product ->
            productsCache.updateProduct(
                product.copy(
                    name = updatedItem.name,
                    price = updatedProduct.subtotalPricePerItem(),
                )
            )
        }
    }

    private suspend fun deleteProductFromCache(productId: Long) {
        productsCache.deleteProduct(productId)
    }

    private fun getProductKey(item: WooPosCartItemViewState.Product): String {
        return when (item) {
            is WooPosCartItemViewState.Product.Simple -> "simple_${item.id}"
            is WooPosCartItemViewState.Product.Variation -> "variation_${item.id}_${item.variationId}"
        }
    }

    private fun findMatchingProduct(
        item: WooPosCartItemViewState.Product,
        updatedProducts: List<ParentToChildrenEvent.OrderCreated.ProductInfo>
    ): ParentToChildrenEvent.OrderCreated.ProductInfo? {
        return when (item) {
            is WooPosCartItemViewState.Product.Simple ->
                updatedProducts.find {
                    it is ParentToChildrenEvent.OrderCreated.ProductInfo.Simple && it.id == item.id
                }

            is WooPosCartItemViewState.Product.Variation ->
                updatedProducts.find { product ->
                    product is ParentToChildrenEvent.OrderCreated.ProductInfo.Variation &&
                        product.id == item.id &&
                        product.variationId == item.variationId
                }
        }
    }

    private fun createAvailableProductsMap(
        updatedProducts: List<ParentToChildrenEvent.OrderCreated.ProductInfo>
    ): MutableMap<String, Int> {
        val productMap = mutableMapOf<String, Int>()

        updatedProducts.forEach { product ->
            val key = when (product) {
                is ParentToChildrenEvent.OrderCreated.ProductInfo.Simple -> "simple_${product.id}"
                is ParentToChildrenEvent.OrderCreated.ProductInfo.Variation ->
                    "variation_${product.id}_${product.variationId}"
            }
            val currentQuantity = productMap[key] ?: 0
            productMap[key] = currentQuantity + product.quantity.toInt()
        }

        return productMap
    }

    private suspend fun updateProductWithNewInfo(
        item: WooPosCartItemViewState.Product,
        updatedProduct: ParentToChildrenEvent.OrderCreated.ProductInfo
    ): WooPosCartItemViewState.Product {
        return when (item) {
            is WooPosCartItemViewState.Product.Simple -> {
                if (updatedProduct is ParentToChildrenEvent.OrderCreated.ProductInfo.Simple) {
                    item.copy(
                        name = updatedProduct.name,
                        price = formatPrice(updatedProduct.subtotalPricePerItem()),
                    )
                } else {
                    item
                }
            }

            is WooPosCartItemViewState.Product.Variation -> {
                if (updatedProduct is ParentToChildrenEvent.OrderCreated.ProductInfo.Variation) {
                    item.copy(
                        name = updatedProduct.name,
                        price = formatPrice(updatedProduct.subtotalPricePerItem())
                    )
                } else {
                    item
                }
            }
        }
    }

    private fun markProductAsNotExisting(
        item: WooPosCartItemViewState.Product
    ): WooPosCartItemViewState.Product {
        return when (item) {
            is WooPosCartItemViewState.Product.Simple -> item.copy(productDoesNotExist = true)
            is WooPosCartItemViewState.Product.Variation -> item.copy(productDoesNotExist = true)
        }
    }

    private fun ParentToChildrenEvent.OrderCreated.ProductInfo.subtotalPricePerItem() =
        basePrice.div(quantity.toBigDecimal())
}
