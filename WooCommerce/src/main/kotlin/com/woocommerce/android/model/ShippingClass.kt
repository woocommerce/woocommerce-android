package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

@Parcelize
data class ShippingClass(
    val name: String = "",
    val slug: String = "",
    val remoteShippingClassId: Long = 0L
) : Parcelable

fun WCProductShippingClassModel.toAppModel(): ShippingClass {
    return ShippingClass(
            name = this.name,
            slug = this.slug,
            remoteShippingClassId = this.remoteShippingClassId
    )
}
