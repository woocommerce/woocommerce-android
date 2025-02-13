package org.wordpress.android.fluxc.model

import com.google.gson.annotations.SerializedName

data class WCProductComponent(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("query_type") val queryType: String,
    @SerializedName("query_ids") val queryIds: List<Long>,
    @SerializedName("default_option_id") val defaultOptionId: String,
    @SerializedName("thumbnail_src") val thumbnailUrl: String?
)
