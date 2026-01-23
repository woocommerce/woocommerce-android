package org.wordpress.android.fluxc.model.pos

import com.google.gson.annotations.SerializedName

data class WooPosGenerateCatalogResponse(
    @SerializedName("scheduled_at")
    val scheduledAt: String? = null,
    @SerializedName("completed_at")
    val completedAt: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("progress")
    val progress: Float? = null,
    @SerializedName("processed")
    val processed: Int? = null,
    @SerializedName("total")
    val total: Int? = null,
    @SerializedName("url")
    val url: String? = null,
)
