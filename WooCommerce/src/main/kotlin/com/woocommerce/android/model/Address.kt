package com.woocommerce.android.model

import android.os.Parcelable
import com.google.i18n.addressinput.common.AddressData
import com.google.i18n.addressinput.common.FormOptions
import com.google.i18n.addressinput.common.FormatInterpreter
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.android.parcel.Parcelize
import java.util.Locale

@Parcelize
data class Address(
    val company: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val country: String,
    val state: String,
    val address1: String,
    val address2: String,
    val city: String,
    val postcode: String,
    val email: String
) : Parcelable {
    private fun orderAddressToString(): String {
        return StringBuilder()
            .appendWithIfNotEmpty(this.address1)
            .appendWithIfNotEmpty(this.address2, "\n")
            .appendWithIfNotEmpty(this.city, "\n")
            .appendWithIfNotEmpty(this.state)
            .appendWithIfNotEmpty(this.postcode)
            .toString()
    }

    private fun getAddressData(): AddressData {
        return AddressData.builder()
            .setAddressLines(mutableListOf(address1, address2))
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
                this.orderAddressToString()
            }
        } ?: this.orderAddressToString()
    }

    fun getFullAddress(name: String, address: String, country: String): String {
        var fullAddr = ""
        if (name.isNotBlank()) fullAddr += "$name\n"
        if (address.isNotBlank()) fullAddr += "$address\n"
        if (country.isNotBlank()) fullAddr += country
        return fullAddr
    }

    fun getCountryLabelByCountryCode(): String {
        val locale = Locale(Locale.getDefault().language, country)
        return locale.displayCountry
    }

    fun hasInfo(): Boolean {
        return firstName.isNotEmpty() || lastName.isNotEmpty() ||
            address1.isNotEmpty() || country.isNotEmpty() ||
            phone.isNotEmpty() || email.isNotEmpty() ||
            state.isNotEmpty() || city.isNotEmpty()
    }

    override fun toString(): String {
        return StringBuilder()
            .appendWithIfNotEmpty("$firstName $lastName")
            .appendWithIfNotEmpty(this.address1,"\n")
            .appendWithIfNotEmpty(this.address2, "\n")
            .appendWithIfNotEmpty(this.city, "\n")
            .appendWithIfNotEmpty(this.state)
            .appendWithIfNotEmpty(this.postcode)
            .appendWithIfNotEmpty(this.country, "\n")
            .toString()
    }
}
