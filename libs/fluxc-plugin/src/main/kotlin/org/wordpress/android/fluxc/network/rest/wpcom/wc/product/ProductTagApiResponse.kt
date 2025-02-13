package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName")
class ProductTagApiResponse : Response {
    var id: Long = 0L

    var name: String? = null
    var slug: String? = null
    var description: String? = null

    val error: JsonElement? = null

    var count = 0
}
