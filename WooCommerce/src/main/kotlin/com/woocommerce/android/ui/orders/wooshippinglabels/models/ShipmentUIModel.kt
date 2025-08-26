package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShipmentUIModel(
    val localId: String,
    val remoteId: String? = null,
    val items: List<ShippableItemModel>,
    val isPurchaseAPILoading: Boolean = false,
    val label: ShippingLabelModel? = null,
) : Parcelable {
    val isPurchasedOrInProgress: Boolean
        get() = label?.status == ShippingLabelStatus.PURCHASE_IN_PROGRESS ||
            (label?.status == ShippingLabelStatus.PURCHASED && label.refund == null)
}
