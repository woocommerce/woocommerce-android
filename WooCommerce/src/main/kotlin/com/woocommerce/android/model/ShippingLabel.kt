package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.ui.orders.shippinglabels.ShipmentTrackingUrls
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class ShippingLabel(
    val id: Long = 0L,
    val trackingNumber: String = "",
    val carrierId: String = "",
    val serviceName: String = "",
    val status: String = "",
    val createdDate: Date? = null,
    val expiryDate: Date? = null,
    val packageName: String = "",
    val rate: BigDecimal = BigDecimal.ZERO,
    val refundableAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "",
    val productNames: List<String> = emptyList(),
    val productIds: List<Long> = emptyList(),
    val originAddress: Address? = null,
    val destinationAddress: Address? = null,
    val refund: Refund? = null,
    val products: List<Order.Item> = emptyList()
) : Parcelable {
    @IgnoredOnParcel
    val trackingLink: String
        get() = ShipmentTrackingUrls.fromCarrier(
            carrierId, trackingNumber
        ) ?: ""

    @IgnoredOnParcel
    val isAnonymized: Boolean
        get() = status == "ANONYMIZED"

    @Parcelize
    data class Refund(
        val status: String,
        val refundDate: Date?
    ) : Parcelable
}

fun WCShippingLabelModel.toAppModel(): ShippingLabel {
    return ShippingLabel(
        id = remoteShippingLabelId,
        trackingNumber = trackingNumber,
        carrierId = carrierId,
        serviceName = serviceName,
        status = status,
        createdDate = dateCreated?.let { Date(it) },
        expiryDate = expiryDate?.let { Date(it) },
        packageName = packageName,
        rate = rate.toBigDecimal(),
        refundableAmount = refundableAmount.toBigDecimal(),
        currency = currency,
        productNames = getProductNameList().map { it.trim() },
        productIds = getProductIdsList(),
        originAddress = getOriginAddress()?.toAppModel(),
        destinationAddress = getDestinationAddress()?.toAppModel(),
        refund = getRefundModel()?.toAppModel()
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
 * Method fetches a list of product details for the product ids or names associated with each shipping label
 *
 * The [ShippingLabel.productIds] field is only available from the API after WCS 1.24.1.
 * Adding a check in this method to fetch products by productIds only if it is available.
 * Otherwise default to using [ShippingLabel.productNames]
 */
fun List<ShippingLabel>.loadProducts(products: List<Order.Item>): List<ShippingLabel> {
    return this.map { shippingLabel ->
        if (shippingLabel.productIds.isNullOrEmpty()) {
            shippingLabel.copy(
                products = products.filter { it.name in shippingLabel.productNames }
            )
        } else {
            shippingLabel.copy(
                products = products.filter { it.uniqueId in shippingLabel.productIds }
            )
        }
    }
}

fun List<ShippingLabel>.getNonRefundedShippingLabelProducts(): List<Order.Item> {
    val nonRefundedProductSet = mutableSetOf<Order.Item>()
    this.filter { it.refund != null }
        .map { nonRefundedProductSet.addAll(it.products) }
    return nonRefundedProductSet.toList()
}
