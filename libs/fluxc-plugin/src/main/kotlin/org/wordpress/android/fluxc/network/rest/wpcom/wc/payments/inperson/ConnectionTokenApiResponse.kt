package org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.inperson

import com.google.gson.annotations.SerializedName

data class ConnectionTokenApiResponse(
    @SerializedName("secret") val token: String,
    @SerializedName("test_mode") val isTestMode: Boolean
)
