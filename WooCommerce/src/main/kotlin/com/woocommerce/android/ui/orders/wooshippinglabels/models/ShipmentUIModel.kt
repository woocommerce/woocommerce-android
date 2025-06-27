package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShipmentUIModel(
    val localId: String,
    val remoteId: String? = null,
    val items: List<ShippableItemModel>,
    val purchased: Boolean = false,
    val purchaseState: PurchaseState = PurchaseState.NoStarted,
    val label: ShippingLabelModel? = null,
) : Parcelable

@Parcelize
sealed class PurchaseState : Parcelable {
    data object NoStarted : PurchaseState()
    data object InProgress : PurchaseState()
    data object Success : PurchaseState()
    data object Error : PurchaseState()
}
