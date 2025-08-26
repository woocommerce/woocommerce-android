package org.wordpress.android.fluxc.model.pos

import com.google.gson.annotations.SerializedName

data class PosGenerateCatalogResponse(
    @SerializedName("job_id")
    val jobId: Long? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("filename")
    val filename: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
)
