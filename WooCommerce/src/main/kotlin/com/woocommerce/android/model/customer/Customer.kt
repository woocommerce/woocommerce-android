package com.woocommerce.android.model.customer

import android.os.Parcelable
import com.woocommerce.android.model.Address
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Customer(
    val avatarUrl: String,
    val billing: Address,
    val dateCreated: String,
    val dateCreatedGmt: String,
    val dateModified: String,
    val dateModifiedGmt: String,
    val email: String,
    val firstName: String,
    val id: Int,
    val isPayingCustomer: Boolean,
    val lastName: String,
    val role: String,
    val shipping: Address,
    val username: String
) : Parcelable
