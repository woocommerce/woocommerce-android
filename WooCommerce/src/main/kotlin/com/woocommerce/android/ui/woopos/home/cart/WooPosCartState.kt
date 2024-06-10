package com.woocommerce.android.ui.woopos.home.cart

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosCartState(
    open val itemsInCart: List<WooPosCartListItem>,
    val areItemsRemovable: Boolean,
    open val isLoading: Boolean,
) : Parcelable {
    data class Cart(
        override val itemsInCart: List<WooPosCartListItem>,
        override val isLoading: Boolean = false,
    ) :
        WooPosCartState(
            itemsInCart = itemsInCart,
            areItemsRemovable = true,
            isLoading = isLoading,
        )

    data class Checkout(
        override val itemsInCart: List<WooPosCartListItem>,
        override val isLoading: Boolean = false,
    ) :
        WooPosCartState(
            itemsInCart = itemsInCart,
            areItemsRemovable = false,
            isLoading = isLoading,
        )
}

@Parcelize
data class WooPosCartListItem(
    val productId: Long,
    val title: String,
) : Parcelable
