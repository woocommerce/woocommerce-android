package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import com.google.gson.annotations.SerializedName

data class SLCreationEligibilityApiResponse(
    @SerializedName("is_eligible")
    val isEligible: Boolean,
    @SerializedName("reason")
    val reason: String?
)
