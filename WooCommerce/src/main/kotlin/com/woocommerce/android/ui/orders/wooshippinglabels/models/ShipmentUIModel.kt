package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShipmentUIModel(
    // If id is null, it indicates that no shipment has been fetched from the backend.
    val id: String?,
    val items: List<ShippableItemModel>
) : Parcelable
