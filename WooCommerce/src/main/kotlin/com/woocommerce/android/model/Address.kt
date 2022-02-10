package com.woocommerce.android.model

import android.os.Parcelable
import com.google.i18n.addressinput.common.AddressData
import com.google.i18n.addressinput.common.FormOptions
import com.google.i18n.addressinput.common.FormatInterpreter
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress

@Suppress("TooManyFunctions")
@Parcelize
data class Address(
    val company: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val country: Location,
    val state: AmbiguousLocation,
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
            .appendWithIfNotEmpty(this.state.codeOrRaw)
            .appendWithIfNotEmpty(this.postcode)
            .toString()
    }

    private fun getAddressData(): AddressData {
        return AddressData.builder()
            .setAddressLines(mutableListOf(address1, address2))
            .setLocality(city)
            .setAdminArea(state.codeOrRaw)
            .setPostalCode(postcode)
            .setCountry(country.code)
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

    fun hasInfo(): Boolean {
        return firstName.isNotEmpty() || lastName.isNotEmpty() ||
            address1.isNotEmpty() || country != Location.EMPTY ||
            phone.isNotEmpty() || email.isNotEmpty() ||
            state.isNotEmpty() || city.isNotEmpty()
    }

    fun toShippingLabelModel(): ShippingLabelAddress {
        return ShippingLabelAddress(
            company = company,
            name = "$firstName $lastName".trim().takeIf { it.isNotBlank() },
            phone = phone,
            address = address1,
            address2 = address2,
            city = city,
            postcode = postcode,
            state = state.codeOrRaw,
            country = country.code
        )
    }

    fun toShippingAddressModel(): OrderAddress.Shipping {
        return OrderAddress.Shipping(
            firstName = firstName,
            lastName = lastName,
            company = company,
            address1 = address1,
            address2 = address2,
            city = city,
            state = state.codeOrRaw,
            postcode = postcode,
            country = country.code,
            phone = phone
        )
    }

    fun toBillingAddressModel(customEmail: String? = null): OrderAddress.Billing {
        return OrderAddress.Billing(
            email = customEmail?.takeIf { it.isNotEmpty() } ?: email,
            firstName = firstName,
            lastName = lastName,
            company = company,
            address1 = address1,
            address2 = address2,
            city = city,
            state = state.codeOrRaw,
            postcode = postcode,
            country = country.code,
            phone = phone
        )
    }

    /**
     * Compares this address's physical location to the other one
     */
    fun isSamePhysicalAddress(otherAddress: Address): Boolean {
        return country.code == otherAddress.country.code &&
            state.codeOrRaw == otherAddress.state.codeOrRaw &&
            address1 == otherAddress.address1 &&
            address2 == otherAddress.address2 &&
            city == otherAddress.city &&
            postcode == otherAddress.postcode
    }

    override fun toString(): String {
        return StringBuilder()
            .appendWithIfNotEmpty(this.company)
            .appendWithIfNotEmpty("$firstName $lastName".trim(), "\n")
            .appendWithIfNotEmpty(this.address1, "\n")
            .appendWithIfNotEmpty(this.address2, "\n")
            .appendWithIfNotEmpty(this.city, "\n")
            .appendWithIfNotEmpty(this.state.codeOrRaw)
            .appendWithIfNotEmpty(this.postcode, " ")
            .appendWithIfNotEmpty(this.country.name, "\n")
            .toString()
    }

    companion object {
        val EMPTY by lazy {
            Address(
                company = "",
                firstName = "",
                lastName = "",
                phone = "",
                country = Location.EMPTY,
                state = AmbiguousLocation.EMPTY,
                address1 = "",
                address2 = "",
                city = "",
                postcode = "",
                email = ""
            )
        }
    }
}
