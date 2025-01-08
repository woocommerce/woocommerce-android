package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
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
    }
}
