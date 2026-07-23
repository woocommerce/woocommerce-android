package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.annotations.SerializedName

data class DuplicateProductApiResponse(
    @SerializedName("id") val id: Long?
)
