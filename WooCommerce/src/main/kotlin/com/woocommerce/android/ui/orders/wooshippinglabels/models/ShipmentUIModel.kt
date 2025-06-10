package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShipmentUIModel(
    val id: String, // Local id
    val remoteId: String? = null,
    val items: List<ShippableItemModel>,
    val purchased: Boolean = false
) : Parcelable
