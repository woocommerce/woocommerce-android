package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.model.Address
import kotlinx.parcelize.Parcelize

@Parcelize
data class OriginShippingAddress(
    val id: String,
    val company: String?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val address1: String?,
    val address2: String?,
    val city: String?,
    val state: String?,
    val postcode: String,
    val country: String,
    val phone: String?,
    val isDefault: Boolean,
    val isVerified: Boolean
) : Parcelable {
    companion object {
        val EMPTY = OriginShippingAddress(
            id = "",
            company = null,
            firstName = null,
            lastName = null,
            email = null,
            address1 = null,
            address2 = null,
            city = null,
            state = null,
            postcode = "",
            country = "",
            phone = null,
            isDefault = false,
            isVerified = false
        )

        fun fromAddress(address: Address) = EMPTY.copy(
            company = address.company,
            firstName = address.firstName,
            lastName = address.lastName,
            email = address.email,
            address1 = address.address1,
            address2 = address.address2,
            city = address.city,
            state = address.state.codeOrRaw,
            postcode = address.postcode,
            country = address.country.code,
            phone = address.phone
        )
    }

    fun format(singleLine: Boolean): String {
        val separator = if (singleLine) ", " else "\n"

        return buildString {
            appendWithIfNotEmpty(address1, separator)
            appendWithIfNotEmpty(address2, separator)
            appendWithIfNotEmpty(city, separator = separator)
            appendWithIfNotEmpty(state, separator = " ")
            appendWithIfNotEmpty(postcode, separator = " ")
            appendWithIfNotEmpty(country, separator = separator)
        }
    }
}
