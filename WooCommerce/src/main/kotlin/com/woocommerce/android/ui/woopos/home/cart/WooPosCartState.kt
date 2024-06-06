package com.woocommerce.android.ui.woopos.home.cart

import android.os.Parcelable
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsListItem
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosCartState(
    open val itemsInCart: List<WooPosProductsListItem>
) : Parcelable {
    data class Cart(override val itemsInCart: List<WooPosProductsListItem>) :
        WooPosCartState(itemsInCart)

    data class Checkout(override val itemsInCart: List<WooPosProductsListItem>) :
        WooPosCartState(itemsInCart)
}
