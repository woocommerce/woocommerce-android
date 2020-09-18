package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.ui.orders.shippinglabels.ShipmentTrackingUrls
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class ShippingLabel(
    val id: Long,
    val trackingNumber: String = "",
    val carrierId: String,
    val serviceName: String,
    val status: String,
    val createdDate: Date?,
    val packageName: String,
    val rate: BigDecimal = BigDecimal.ZERO,
    val refundableAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String,
    val paperSize: String,
    val productNames: List<String>,
    val originAddress: Address? = null,
    val destinationAddress: Address? = null,
    val refund: Refund? = null
) : Parcelable {
    @IgnoredOnParcel
    var trackingLink: String? = null

    @Parcelize
    data class Refund(
        val status: String,
        val refundDate: Date?
    ) : Parcelable
}

fun WCShippingLabelModel.toAppModel(): ShippingLabel {
    return ShippingLabel(
        remoteShippingLabelId,
        trackingNumber,
        carrierId,
        serviceName,
        status,
        Date(dateCreated.toLong()),
        packageName,
        rate.toBigDecimal(),
        refundableAmount.toBigDecimal(),
        currency,
        paperSize,
        getProductNameList().map { it.trim() },
        getOriginAddress()?.toAppModel(),
        getDestinationAddress()?.toAppModel(),
        getRefundModel()?.toAppModel()
    )
}

fun WCShippingLabelModel.ShippingLabelAddress.toAppModel(): Address {
    return Address(
        company = company ?: "",
        firstName = name ?: "",
        lastName = "",
        phone = phone ?: "",
        country = country ?: "",
        state = state ?: "",
        address1 = address ?: "",
        address2 = address2 ?: "",
        city = city ?: "",
        postcode = postcode ?: "",
        email = ""
    )
}

fun WCShippingLabelModel.WCShippingLabelRefundModel.toAppModel(): ShippingLabel.Refund {
    return ShippingLabel.Refund(
        status ?: "",
        requestDate?.let { Date(it) }
    )
}

/**
 * Method provides a list of [Order.Item] for the given [ShippingLabel.productNames]
 * in a shipping label.
 *
 * Used to display the list of products associated with a shipping label
 */
fun ShippingLabel.loadProductItems(orderItems: List<Order.Item>) =
    orderItems.filter { it.name in productNames }

/**
 * Shipment tracking links are not available by default from the shipping label API.
 * Until this is available on the API side, we need to fetch the tracking link from the
 * shipment tracking API (if available) and link the tracking link to the corresponding
 * tracking number of a shipping label.
 *
 * In cases where the ST plugin is not available, we need to fetch the tracking url based on the
 * carrierId, mapped in [ShipmentTrackingUrls]. This is currently how WCS is handling it
*/
fun List<ShippingLabel>.appendTrackingUrls(
    orderShipmentTrackings: List<WCOrderShipmentTrackingModel>
): List<ShippingLabel> {
    this.map { shippingLabel ->
        if (orderShipmentTrackings.isEmpty()) {
            shippingLabel.trackingLink = ShipmentTrackingUrls.fromCarrier(
                shippingLabel.carrierId, shippingLabel.trackingNumber
            )
        } else {
            orderShipmentTrackings.forEach { shipmentTracking ->
                shippingLabel.trackingLink = if (shipmentTracking.trackingNumber == shippingLabel.trackingNumber) {
                    shipmentTracking.trackingLink
                } else {
                    ShipmentTrackingUrls.fromCarrier(
                        shippingLabel.carrierId, shippingLabel.trackingNumber
                    )
                }
            }
        }
    }
    return this
}
