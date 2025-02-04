package org.wordpress.android.fluxc.network.rest.wpcom.wc

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.Response

class SiteSettingsResponse : Response {
    val id: String? = null
    val value: JsonElement? = null // JsonElement because this field can be a string or an array (at least)
}
