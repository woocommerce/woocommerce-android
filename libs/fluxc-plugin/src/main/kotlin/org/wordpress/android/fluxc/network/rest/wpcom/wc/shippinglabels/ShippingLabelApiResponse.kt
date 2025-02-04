package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.Response

class ShippingLabelApiResponse : Response {
    val orderId: Long? = null
    val paperSize: String? = null

    val formData: JsonElement? = null
    val storeOptions: JsonElement? = null
    val labelsData: List<LabelItem>? = null

    val success: Boolean? = null
}
