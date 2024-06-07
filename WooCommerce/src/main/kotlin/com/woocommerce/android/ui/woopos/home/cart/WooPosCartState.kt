package com.woocommerce.android.ui.woopos.home.cart

import android.os.Parcelable
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsListItem
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosCartState(
    open val itemsInCart: List<WooPosProductsListItem>,
    val areItemsRemovable: Boolean,
) : Parcelable {
    data class Cart(override val itemsInCart: List<WooPosProductsListItem>) :
        WooPosCartState(
            itemsInCart = itemsInCart,
            areItemsRemovable = true,
        )

    data class Checkout(override val itemsInCart: List<WooPosProductsListItem>) :
        WooPosCartState(
            itemsInCart = itemsInCart,
            areItemsRemovable = false,
        )
}
