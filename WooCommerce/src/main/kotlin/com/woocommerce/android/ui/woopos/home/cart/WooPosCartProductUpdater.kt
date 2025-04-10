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
                    val availableQuantity = availableProductsMap[item.id] ?: 0

                    if (availableQuantity > 0) {
                        availableProductsMap[item.id] = availableQuantity - 1

                        val updatedProduct = updatedProducts.find { it.id == item.id }

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
                        mutableCurrentBodyList[index] = markProductAsNotExisting(item)
                        changesDone = true
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

    private fun createAvailableProductsMap(
        updatedProducts: List<ParentToChildrenEvent.OrderCreated.ProductInfo>
    ): MutableMap<Long, Int> {
        val productMap = mutableMapOf<Long, Int>()

        updatedProducts.forEach { product ->
            val currentQuantity = productMap[product.id] ?: 0
            productMap[product.id] = currentQuantity + product.quantity.toInt()
        }

        return productMap
    }

    private suspend fun updateProductWithNewInfo(
        item: WooPosCartItemViewState.Product,
        updatedProduct: ParentToChildrenEvent.OrderCreated.ProductInfo
    ): WooPosCartItemViewState.Product {
        return when (item) {
            is WooPosCartItemViewState.Product.Simple -> {
                item.copy(
                    name = updatedProduct.name,
                    price = formatPrice(updatedProduct.price)
                )
            }
            is WooPosCartItemViewState.Product.Variation -> {
                item.copy(
                    name = updatedProduct.name,
                    price = formatPrice(updatedProduct.price)
                )
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
