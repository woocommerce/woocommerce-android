package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCShippingMethod

@Parcelize
data class ShippingMethod(
    val id: String,
    val title: String
) : Parcelable

fun WCShippingMethod.toAppModel(): ShippingMethod {
    return ShippingMethod(id = this.id, title = this.title)
}
