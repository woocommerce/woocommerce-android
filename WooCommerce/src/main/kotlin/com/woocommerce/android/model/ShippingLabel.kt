package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import java.math.BigDecimal

@Parcelize
data class ShippingLabel(
    val id: Long,
    val trackingNumber: String = "",
    val carrierId: String,
    val serviceName: String,
    val status: String,
    val packageName: String,
    val rate: BigDecimal = BigDecimal.ZERO,
    val refundableAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String,
    val paperSize: String,
    val productNames: List<String>,
    val originAddress: Address? = null,
    val destinationAddress: Address? = null
) : Parcelable {
    @Parcelize
    data class Address(
        val company: String,
        val name: String,
        val phone: String,
        val country: String,
        val state: String,
        val address: String,
        val address2: String,
        val city: String,
        val postcode: String
    ) : Parcelable
}

fun WCShippingLabelModel.toAppModel(): ShippingLabel {
    return ShippingLabel(
        remoteShippingLabelId,
        trackingNumber,
        carrierId,
        serviceName,
        status,
        packageName,
        rate.toBigDecimal(),
        refundableAmount.toBigDecimal(),
        currency,
        paperSize,
        getProductNames(),
        getOriginAddress()?.toAppModel(),
        getDestinationAddress()?.toAppModel()
    )
}

fun WCShippingLabelModel.ShippingLabelAddress.toAppModel(): ShippingLabel.Address {
    return ShippingLabel.Address(
        company ?: "",
        name ?: "",
        phone ?: "",
        country ?: "",
        state ?: "",
        address ?: "",
        address2 ?: "",
        city ?: "",
        postcode ?: ""
    )
}
