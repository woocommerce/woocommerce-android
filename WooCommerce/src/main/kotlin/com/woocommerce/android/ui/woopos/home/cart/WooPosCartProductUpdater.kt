package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WooPosCartProductUpdater @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val resourceProvider: ResourceProvider,
    private val formatPrice: WooPosFormatPrice
) {
    suspend operator fun invoke(
        currentState: WooPosCartState,
        updatedProducts: List<ParentToChildrenEvent.OrderCreated.ProductInfo>,
    ): WooPosCartState {
        val currentBodyState = currentState.body as? WooPosCartState.Body.WithItems ?: return currentState
        val mutableCurrentBodyList = currentBodyState.itemsInCart.toMutableList()
        var changesDone = false

        val availableProductsMap = createAvailableProductsMap(updatedProducts)

        currentBodyState.itemsInCart.forEachIndexed { index, item ->
            when (item) {
                is WooPosCartItemViewState.Product -> {
                    val productKey = getProductKey(item)
                    val availableQuantity = availableProductsMap[productKey] ?: 0

                    if (availableQuantity > 0) {
                        availableProductsMap[productKey] = availableQuantity - 1

                        val updatedProduct = findMatchingProduct(item, updatedProducts)

                        if (updatedProduct != null) {
                            val updatedItem = updateProductWithNewInfo(
                                item = item,
                                updatedProduct = updatedProduct
                            )
                            val itemChanged = (updatedItem.name != item.name || updatedItem.price != item.price)

                            mutableCurrentBodyList[index] = updatedItem
                            changesDone = changesDone || itemChanged
                        }
                    } else {
                        val updatedItem = markProductAsNotExisting(item)
                        mutableCurrentBodyList[index] = updatedItem
                        val itemChanged = (
                            updatedItem.name != item.name ||
                                updatedItem.price != item.price ||
                                updatedItem.productDoesNotExist != item.productDoesNotExist
                            )
                        changesDone = changesDone || itemChanged
                    }
                }

                is WooPosCartItemViewState.Coupon -> {
                    // We may need to update the coupon in the future
                }
            }
        }

        if (changesDone) {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.ToastMessageDisplayed(
                    message = resourceProvider.getString(R.string.woopos_cart_changes_in_the_cart)
                )
            )
        }

        return currentState.copy(
            body = currentBodyState.copy(
                itemsInCart = mutableCurrentBodyList
            )
        )
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
                        price = formatPrice(updatedProduct.price)
                    )
                } else {
                    item
                }
            }

            is WooPosCartItemViewState.Product.Variation -> {
                if (updatedProduct is ParentToChildrenEvent.OrderCreated.ProductInfo.Variation) {
                    item.copy(
                        name = updatedProduct.name,
                        price = formatPrice(updatedProduct.price)
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
            is WooPosCartItemViewState.Product.Simple -> {
                item.copy(productDoesNotExist = true)
            }

            is WooPosCartItemViewState.Product.Variation -> {
                item.copy(productDoesNotExist = true)
            }
        }
    }
}
