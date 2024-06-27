package com.woocommerce.android.ui.woopos.home.cart

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.woocommerce.android.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosCartState(
    val cartStatus: WooPosCartStatus = WooPosCartStatus.EDITABLE,
    val toolbar: WooPosCartToolbar = WooPosCartToolbar(),
    val itemsInCart: List<WooPosCartListItem> = emptyList(),
    val areItemsRemovable: Boolean = true,
    val isOrderCreationInProgress: Boolean = false,
    val isCheckoutButtonVisible: Boolean = true,
) : Parcelable

@Parcelize
data class WooPosCartListItem(
    val id: Id,
    val name: String,
    val price: String,
    val imageUrl: String?,
) : Parcelable {
    @Parcelize
    data class Id(val productId: Long, val itemNumber: Int) : Parcelable
}

@Parcelize
data class WooPosCartToolbar(
    @DrawableRes val icon: Int = R.drawable.ic_shopping_cart,
    val itemsCount: String = "",
    val isClearAllButtonVisible: Boolean = false,
) : Parcelable

enum class WooPosCartStatus {
    EDITABLE, CHECKOUT
}
