package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShippingLabelPackage(
    val packageId: String,
    val selectedPackage: ShippingPackage?,
    val weight: Float,
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
