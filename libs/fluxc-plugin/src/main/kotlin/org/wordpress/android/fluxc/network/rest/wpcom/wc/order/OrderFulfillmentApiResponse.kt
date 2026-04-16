package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

class OrderFulfillmentApiResponse : Response {
    val id: Long? = null
    val status: String? = null

    @SerializedName("is_fulfilled")
    val isFulfilled: Boolean? = null

    @SerializedName("date_updated")
    val dateUpdated: String? = null

    @SerializedName("meta_data")
    val metaData: JsonElement? = null
}
