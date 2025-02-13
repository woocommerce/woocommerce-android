package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

data class CustomerDTO(
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("billing") val billing: Billing? = null,
    @SerializedName("date_created") val dateCreated: String? = null,
    @SerializedName("date_created_gmt") val dateCreatedGmt: String? = null,
    @SerializedName("date_modified") val dateModified: String? = null,
    @SerializedName("date_modified_gmt") val dateModifiedGmt: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("id") val id: Long? = null,
    @SerializedName("is_paying_customer") val isPayingCustomer: Boolean = false,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("shipping") val shipping: Shipping? = null,
    @SerializedName("username") val username: String? = null
) : Response {
    data class Billing(
        @SerializedName("address_1") val address1: String? = null,
        @SerializedName("address_2") val address2: String? = null,
        @SerializedName("city") val city: String? = null,
        @SerializedName("company") val company: String? = null,
        @SerializedName("country") val country: String? = null,
        @SerializedName("email") val email: String? = null,
        @SerializedName("first_name") val firstName: String? = null,
        @SerializedName("last_name") val lastName: String? = null,
        @SerializedName("phone") val phone: String? = null,
        @SerializedName("postcode") val postcode: String? = null,
        @SerializedName("state") val state: String? = null
    )

    data class Shipping(
        @SerializedName("address_1") val address1: String? = null,
        @SerializedName("address_2") val address2: String? = null,
        @SerializedName("city") val city: String? = null,
        @SerializedName("company") val company: String? = null,
        @SerializedName("country") val country: String? = null,
        @SerializedName("first_name") val firstName: String? = null,
        @SerializedName("last_name") val lastName: String? = null,
        @SerializedName("postcode") val postcode: String? = null,
        @SerializedName("state") val state: String? = null
    )
}
