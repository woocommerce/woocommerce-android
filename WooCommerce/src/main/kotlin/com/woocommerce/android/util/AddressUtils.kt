package com.woocommerce.android.util

import com.google.i18n.addressinput.common.AddressData
import com.google.i18n.addressinput.common.FormOptions
import com.google.i18n.addressinput.common.FormatInterpreter
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.model.order.OrderAddress
import java.util.Locale

object AddressUtils {
    /**
     * Translates a two-character country code into a human
     * readable label.
     *
     * Example: US -> United States
     */
    fun getCountryLabelByCountryCode(countryCode: String): String {
        val locale = Locale(Locale.getDefault().language, countryCode)
        return locale.displayCountry
    }

    /**
     * Converts an [OrderAddress] object into the i18n [AddressData]
     * object for localization and formatting.
     */
    private fun getAddressData(address: OrderAddress): AddressData {
        return AddressData.builder()
                .setAddressLines(mutableListOf(address.address1, address.address2))
                .setLocality(address.city)
                .setAdminArea(address.state)
                .setPostalCode(address.postcode)
                .setCountry(address.country)
                .setOrganization(address.company)
                .build()
    }

    /**
     * Converts and [OrderAddress] object into a string formatted properly for its
     * country. Includes line breaks so using with a text view is a cinch. Prints out
     * what would be needed if addressing an envelope.
     *
     * The "company" will only be included in the output if it exists.
     *
     * Example:
     *      Ramada Plaza
     *      450 Capitol Ave SE
     *      Atlanta, GA 30312
     *
     * @return If the provided [address] has an empty country code, this method will return whatever
     * address fields are available, otherwise, return the address formatted for the region.
     */
    fun getEnvelopeAddress(address: OrderAddress): String {
        return getAddressData(address).takeIf { it.postalCountry != null }?.let {
            val formatInterpreter = FormatInterpreter(FormOptions().createSnapshot())
            try {
                val separator = System.getProperty("line.separator") ?: ""
                formatInterpreter.getEnvelopeAddress(it).joinToString(separator)
            } catch (e: NullPointerException) {
                // in rare cases getEnvelopeAddress() will throw a NPE due to invalid region data
                // see https://github.com/woocommerce/woocommerce-android/issues/509
                WooLog.e(T.UTILS, e)
                orderAddressToString(address)
            }
        } ?: orderAddressToString(address)
    }

    /**
     * Takes an [OrderAddress] object and returns its values in a comma-separated string.
     */
    private fun orderAddressToString(address: OrderAddress): String {
        return StringBuilder()
                .appendWithIfNotEmpty(address.address1)
                .appendWithIfNotEmpty(address.address2, "\n")
                .appendWithIfNotEmpty(address.city, "\n")
                .appendWithIfNotEmpty(address.state)
                .appendWithIfNotEmpty(address.postcode)
                .toString()
    }

    fun getFullAddress(name: String, address: String, country: String): String {
        var fullAddr = if (!name.isBlank()) "$name\n" else ""
        if (!address.isBlank()) fullAddr += "$address\n"
        if (!country.isBlank()) fullAddr += country
        return fullAddr
    }
}
