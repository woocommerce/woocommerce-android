package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel

@Parcelize
data class OrderShipmentProvider(
    val carrierName: String,
    val carrierLink: String,
    val country: String
) : Parcelable

fun WCOrderShipmentProviderModel.toAppModel() = OrderShipmentProvider(
    carrierName = carrierName,
    carrierLink = carrierLink,
    country = country
)
