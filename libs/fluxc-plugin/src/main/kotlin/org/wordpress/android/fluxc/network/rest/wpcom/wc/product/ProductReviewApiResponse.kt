package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName", "VariableNaming")
class ProductReviewApiResponse : Response {
    val id: Long = 0L
    val date_created_gmt: String? = null
    val product_id: Long = 0L
    val status: String? = null
    val reviewer: String? = null
    val reviewer_email: String? = null
    val review: String? = null
    val rating: Int = 0
    val verified: Boolean = false
    val reviewer_avatar_urls: JsonElement? = null
}
