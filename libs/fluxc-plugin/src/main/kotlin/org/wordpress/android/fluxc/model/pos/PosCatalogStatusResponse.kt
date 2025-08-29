package org.wordpress.android.fluxc.model.pos

import com.google.gson.annotations.SerializedName

data class PosCatalogStatusResponse(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("download_url")
    val downloadUrl: String? = null,
)
