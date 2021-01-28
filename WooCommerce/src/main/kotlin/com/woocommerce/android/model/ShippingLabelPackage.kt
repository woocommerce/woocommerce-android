package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ShippingLabelPackage(
    val selectedPackage: ShippingPackage,
    val weight: Int,
    val items: List<Item>
) : Parcelable {
    @Parcelize
    data class Item(
        val productId: Long,
        val name: String,
        val attributesList: String,
        val weight: String
    ) : Parcelable
}
