package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.ui.orders.shippinglabels.ShipmentTrackingUrls
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.TimeUnit

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
    val products: List<Order.Item> = emptyList(),
    val commercialInvoiceUrl: String?
) : Parcelable {
    @IgnoredOnParcel
    val trackingLink: String
        get() = ShipmentTrackingUrls.fromCarrier(
            carrierId, trackingNumber
        ) ?: ""

    /**
     * Checks if a label has been anonymized.
     * An label gets anonymized when the store owner request its data to be removed from the server, for privacy reasons
     */
    @IgnoredOnParcel
    val isAnonymized: Boolean
        get() = status == "ANONYMIZED"

    @IgnoredOnParcel
    val refundExpiryDate: Date?
        get() = createdDate?.let { Date(it.time + TimeUnit.DAYS.toMillis(30)) }

    val hasCommercialInvoice
        get() = !commercialInvoiceUrl.isNullOrEmpty()

    @Parcelize
    data class Refund(
        val status: String,
        val refundDate: Date?
    ) : Parcelable
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
