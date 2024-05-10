package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.network.shippingmethods.ShippingMethodsRestClient
import com.woocommerce.android.ui.orders.creation.shipping.ShippingMethodsRepository
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShippingMethod(
    val id: String,
    val title: String
) : Parcelable

fun ShippingMethodsRestClient.ShippingMethodDto.toAppModel(): ShippingMethod {
    return ShippingMethod(id = this.id ?: ShippingMethodsRepository.OTHER_ID, title = this.title.orEmpty())
}
