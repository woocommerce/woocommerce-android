package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName", "VariableNaming")
class OrderFulfillmentApiResponse : Response {
    val id: Long? = null
    val status: String? = null
    val is_fulfilled: Boolean? = null
    val date_updated: String? = null
    val meta_data: JsonElement? = null
}
