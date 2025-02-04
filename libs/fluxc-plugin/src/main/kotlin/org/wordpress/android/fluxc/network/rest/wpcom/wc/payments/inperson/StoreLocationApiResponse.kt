package org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.inperson

import com.google.gson.annotations.SerializedName

data class StoreLocationApiResponse(
    @SerializedName("id") val id: String,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("livemode") val liveMode: Boolean?,
    @SerializedName("address") val address: StoreAddress?
) {
    data class StoreAddress(
        @SerializedName("city") val city: String?,
        @SerializedName("country") val country: String?,
        @SerializedName("line1") val line1: String?,
        @SerializedName("line2") val line2: String?,
        @SerializedName("postal_code") val postalCode: String?,
        @SerializedName("state") val state: String?
    )
}
