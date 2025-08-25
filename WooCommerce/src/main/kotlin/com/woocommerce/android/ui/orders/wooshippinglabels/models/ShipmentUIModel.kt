package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShipmentUIModel(
    val localId: String,
    val remoteId: String? = null,
    val items: List<ShippableItemModel>,
    val purchaseState: PurchaseState = PurchaseState.NoStarted,
    val label: ShippingLabelModel? = null,
) : Parcelable {
    /**
     * Whether the shipment has been purchased or not.
     * A shipment is considered purchased if the label was already purchased or is in the process of being purchased.
     */
    val purchased: Boolean
        get() = label?.status == ShippingLabelStatus.PURCHASE_IN_PROGRESS ||
            (label?.status == ShippingLabelStatus.PURCHASED && label.refund == null)
}

@Parcelize
sealed class PurchaseState : Parcelable {
    data object NoStarted : PurchaseState()
    data object InProgress : PurchaseState()
    data object Success : PurchaseState()
    data object Error : PurchaseState()
}
