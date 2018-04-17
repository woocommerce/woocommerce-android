package com.woocommerce.android.util

import com.google.i18n.addressinput.common.AddressData
import com.google.i18n.addressinput.common.FormOptions
import com.google.i18n.addressinput.common.FormatInterpreter
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
     */
    fun getEnvelopeAddress(address: OrderAddress): String {
        val addressData = getAddressData(address)
        val formatInterpreter = FormatInterpreter(FormOptions().createSnapshot())
        return formatInterpreter.getEnvelopeAddress(addressData).joinToString(System.getProperty("line.separator"))
    }
}
