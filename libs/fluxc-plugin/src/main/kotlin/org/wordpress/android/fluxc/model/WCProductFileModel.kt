package org.wordpress.android.fluxc.model

import com.google.gson.JsonObject

/**
 * This class represents a downloadable file:
 *      http://woocommerce.github.io/woocommerce-rest-api-docs/?shell#product-downloads-properties
 *
 * The id is nullable as during the creation of downloadable file, we send the payload without any id, and we get the
 * created ID in the response.
 */
data class WCProductFileModel(val id: String? = null, val name: String = "", val url: String = "") {
    fun toJson() = JsonObject().apply {
        if (id != null) addProperty("id", id)
        addProperty("name", name)
        addProperty("file", url)
    }
}
