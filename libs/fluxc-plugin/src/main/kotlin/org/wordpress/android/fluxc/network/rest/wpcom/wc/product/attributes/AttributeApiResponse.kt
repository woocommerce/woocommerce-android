package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

class AttributeApiResponse : Response {
    val id: String? = null
    val name: String? = null
    val slug: String? = null
    val type: String? = null
    @SerializedName("order_by")
    val orderBy: String? = null
    @SerializedName("has_archives")
    val hasArchives: Boolean? = null
}
