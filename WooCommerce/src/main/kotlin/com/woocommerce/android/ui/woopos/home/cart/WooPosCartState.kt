package com.woocommerce.android.ui.woopos.home.cart

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosCartState(
    open val itemsInCart: List<WooPosCartListItem>,
    val areItemsRemovable: Boolean,
) : Parcelable {
    data class Cart(override val itemsInCart: List<WooPosCartListItem>) :
        WooPosCartState(
            itemsInCart = itemsInCart,
            areItemsRemovable = true,
        )

    data class Checkout(override val itemsInCart: List<WooPosCartListItem>) :
        WooPosCartState(
            itemsInCart = itemsInCart,
            areItemsRemovable = false,
        )
}

@Parcelize
data class WooPosCartListItem(
    val productId: Long,
    val title: String,
) : Parcelable
