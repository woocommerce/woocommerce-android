package com.woocommerce.android.model

import android.os.Parcelable
import com.google.i18n.addressinput.common.AddressData
import com.google.i18n.addressinput.common.FormOptions
import com.google.i18n.addressinput.common.FormatInterpreter
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.ui.orders.shippinglabels.ShipmentTrackingUrls
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
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
    ) : Parcelable {
        /**
         * Takes an [ShippingLabel.Address] object and returns its values in a comma-separated string.
         */
        private fun addressToString(): String {
            return StringBuilder()
                .appendWithIfNotEmpty(address)
                .appendWithIfNotEmpty(address2, "\n")
                .appendWithIfNotEmpty(city, "\n")
                .appendWithIfNotEmpty(state)
                .appendWithIfNotEmpty(postcode)
                .toString()
        }

        private fun getAddressData(): AddressData {
            return AddressData.builder()
                .setAddressLines(mutableListOf(address, address2))
                .setLocality(city)
                .setAdminArea(state)
                .setPostalCode(postcode)
                .setCountry(country)
                .setOrganization(company)
                .build()
        }

        fun getEnvelopeAddress(): String {
            return getAddressData().takeIf { it.postalCountry != null }?.let {
                val formatInterpreter = FormatInterpreter(FormOptions().createSnapshot())
                try {
                    val separator = System.getProperty("line.separator") ?: ""
                    formatInterpreter.getEnvelopeAddress(it).joinToString(separator)
                } catch (e: NullPointerException) {
                    // in rare cases getEnvelopeAddress() will throw a NPE due to invalid region data
                    // see https://github.com/woocommerce/woocommerce-android/issues/509
                    WooLog.e(T.UTILS, e)
                    this.addressToString()
                }
            } ?: this.addressToString()
        }
    }

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
        getProductNames().map { it.trim() },
        getOriginAddress()?.toAppModel(),
        getDestinationAddress()?.toAppModel(),
        getRefund()?.toAppModel()
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
                if (shipmentTracking.trackingNumber == shippingLabel.trackingNumber) {
                    shippingLabel.trackingLink = shipmentTracking.trackingLink
                }
            }
        }
    }
    return this
}
