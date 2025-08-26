package org.wordpress.android.fluxc.model.pos

import com.google.gson.annotations.SerializedName

data class PosCatalogStatusResponse(
    @SerializedName("job_id")
    val jobId: Long? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("filename")
    val filename: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("progress")
    val progress: Int? = null,
    @SerializedName("download_url")
    val downloadUrl: String? = null,
)
