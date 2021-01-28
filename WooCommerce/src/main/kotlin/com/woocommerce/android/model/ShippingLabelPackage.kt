package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.model.Order.Item
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ShippingLabelPackage(
    val selectedPackage: ShippingPackage,
    val weight: Int,
    val items: List<Item>
) : Parcelable

