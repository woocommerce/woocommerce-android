package com.woocommerce.android.model.customer

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CustomerBilling(
    val address1: String,
    val address2: String,
    val city: String,
    val company: String,
    val country: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val postcode: String,
    val state: String
) : Parcelable
