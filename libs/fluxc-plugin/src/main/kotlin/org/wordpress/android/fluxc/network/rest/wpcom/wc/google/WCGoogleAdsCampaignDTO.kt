package org.wordpress.android.fluxc.network.rest.wpcom.wc.google

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

data class WCGoogleAdsCampaignDTO(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("type") val rawType: String? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("targeted_locations") val targetedLocations: List<String>? = null
) : Response
