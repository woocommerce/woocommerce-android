package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

class BatchAddProductTagApiResponse : Response {
    @SerializedName("create")
    val addedTags: List<ProductTagApiResponse>? = null
}
