package org.wordpress.android.fluxc.network.rest.wpcom.wc

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

class SiteSettingOptionResponse : Response {
    @SerializedName("id") val id: String? = null
    @SerializedName("label") val label: String? = null
    @SerializedName("description") val description: String? = null
    @SerializedName("type") val type: String? = null
    @SerializedName("default") val default: String? = null
    @SerializedName("options") val options: JsonElement? = null
    @SerializedName("tip") val tip: String? = null
    @SerializedName("value") val value: String? = null
}
