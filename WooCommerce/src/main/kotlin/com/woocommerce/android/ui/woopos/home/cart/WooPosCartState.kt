package com.woocommerce.android.ui.woopos.home.cart

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosCartState(
    val cartStatus: WooPosCartStatus = WooPosCartStatus.EDITABLE,
    val toolbar: WooPosCartToolbar = WooPosCartToolbar(),
    val body: WooPosCartBody = WooPosCartBody(),
    val areItemsRemovable: Boolean = true,
    val isCheckoutButtonVisible: Boolean = true,
) : Parcelable

@Parcelize
data class WooPosCartBody(
    val itemsInCart: List<WooPosCartListItem> = emptyList(),
    @DrawableRes val icon: Int? = null,
    val text: String = "",
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
    @DrawableRes val icon: Int? = null,
    val itemsCount: String = "",
    val isClearAllButtonVisible: Boolean = false,
) : Parcelable

enum class WooPosCartStatus {
    EDITABLE, CHECKOUT, EMPTY,
}
