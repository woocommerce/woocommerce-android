package com.woocommerce.android.ui.woopos.home.cart

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosCartState(
    val itemsInCart: List<WooPosCartListItem> = emptyList(),
    val areItemsRemovable: Boolean = true,
    val isOrderCreationInProgress: Boolean = false,
    val isCheckoutButtonVisible: Boolean = true,
) : Parcelable

@Parcelize
data class WooPosCartListItem(
    val productId: Long,
    val title: String,
) : Parcelable
