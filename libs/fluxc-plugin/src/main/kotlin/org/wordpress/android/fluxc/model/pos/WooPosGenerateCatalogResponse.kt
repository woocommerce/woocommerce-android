package org.wordpress.android.fluxc.model.pos

import com.google.gson.annotations.SerializedName

data class WooPosGenerateCatalogResponse(
    @SerializedName("job_id")
    val jobId: Long? = null,
)
