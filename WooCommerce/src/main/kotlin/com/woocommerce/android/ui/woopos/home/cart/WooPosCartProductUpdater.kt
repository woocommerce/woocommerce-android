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

        val expandedUpdatedProductsBasedOnQuantity = updatedProducts.flatMap { product ->
            List(product.quantity.toInt()) { product }
        }.toMutableList()

        currentBodyState.itemsInCart.forEachIndexed { index, item ->
            when (item) {
                is WooPosCartItemViewState.Product -> {
                    val updatedProductIndex = expandedUpdatedProductsBasedOnQuantity.indexOfFirst { updatedProduct ->
                        val productExists = updatedProduct.id == item.id && updatedProduct.id != 0L
                        val productDoesNotExist = updatedProduct.id == 0L && updatedProduct.name == item.name
                        productExists || productDoesNotExist
                    }

                    if (updatedProductIndex != -1) {
                        val updatedProduct = expandedUpdatedProductsBasedOnQuantity[updatedProductIndex]
                        expandedUpdatedProductsBasedOnQuantity.removeAt(updatedProductIndex)

                        val productExists = updatedProduct.id == item.id && updatedProduct.id != 0L
                        val productDoesNotExist = updatedProduct.id == 0L && updatedProduct.name == item.name

                        mutableCurrentBodyList[index] = when (item) {
                            is WooPosCartItemViewState.Product.Simple -> {
                                when {
                                    productExists -> {
                                        val newItem = item.copy(
                                            name = updatedProduct.name,
                                            price = formatPrice(updatedProduct.price)
                                        )
                                        val itemChanged = newItem.name != item.name || newItem.price != item.price
                                        changesDone = changesDone || itemChanged
                                        newItem
                                    }
                                    productDoesNotExist -> {
                                        val newItem = item.copy(
                                            price = formatPrice(updatedProduct.price),
                                            productDoesNotExist = false
                                        )
                                        changesDone = true
                                        newItem
                                    }
                                    else -> item
                                }
                            }
                            is WooPosCartItemViewState.Product.Variation -> {
                                when {
                                    productExists -> {
                                        val newItem = item.copy(
                                            name = updatedProduct.name,
                                            price = formatPrice(updatedProduct.price)
                                        )
                                        val itemChanged = newItem.name != item.name || newItem.price != item.price
                                        changesDone = changesDone || itemChanged
                                        newItem
                                    }
                                    productDoesNotExist -> {
                                        val newItem = item.copy(
                                            price = formatPrice(updatedProduct.price),
                                            productDoesNotExist = false
                                        )
                                        changesDone = true
                                        newItem
                                    }
                                    else -> item
                                }
                            }
                        }
                    }
                }
                is WooPosCartItemViewState.Coupon -> {
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
}
