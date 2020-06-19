package com.woocommerce.android.model

import android.os.Parcelable
import com.google.i18n.addressinput.common.AddressData
import com.google.i18n.addressinput.common.FormOptions
import com.google.i18n.addressinput.common.FormatInterpreter
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
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

fun ShippingLabel.Address.getEnvelopeAddress(): String {
    return this.getAddressData().takeIf { it.postalCountry != null }?.let {
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

private fun ShippingLabel.Address.getAddressData(): AddressData {
    return AddressData.builder()
        .setAddressLines(mutableListOf(address, address2))
        .setLocality(city)
        .setAdminArea(state)
        .setPostalCode(postcode)
        .setCountry(country)
        .setOrganization(company)
        .build()
}

/**
 * Takes an [ShippingLabel.Address] object and returns its values in a comma-separated string.
 */
private fun ShippingLabel.Address.addressToString(): String {
    return StringBuilder()
        .appendWithIfNotEmpty(address)
        .appendWithIfNotEmpty(address2, "\n")
        .appendWithIfNotEmpty(city, "\n")
        .appendWithIfNotEmpty(state)
        .appendWithIfNotEmpty(postcode)
        .toString()
}
