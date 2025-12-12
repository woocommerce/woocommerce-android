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
    val progress: Int? = null,
    @SerializedName("processed")
    val processed: Int? = null,
    @SerializedName("total")
    val total: Int? = null,
    @SerializedName("args")
    val args: WooPosGenerateCatalogArgs? = null,
    @SerializedName("url")
    val url: String? = null,
)

data class WooPosGenerateCatalogArgs(
    @SerializedName("_product_fields")
    val productFields: List<String>? = null,
    @SerializedName("_variation_fields")
    val variationFields: List<String>? = null,
)
