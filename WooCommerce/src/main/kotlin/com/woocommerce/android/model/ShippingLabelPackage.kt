package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

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
        val quantity: Float,
        val weight: Float,
        val value: BigDecimal
    ) : Parcelable
}
