package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.terms

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

class AttributeTermApiResponse : Response {
    val id: String? = null
    val name: String? = null
    val slug: String? = null
    val description: String? = null
    val count: String? = null
    @SerializedName("menu_order")
    val menuOrder: String? = null
}
