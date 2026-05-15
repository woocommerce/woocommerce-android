package org.wordpress.android.fluxc.network.discovery

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.JsonObjectOrNull

class RootWPAPIRestResponse(
    val name: String? = null,
    val description: String? = null,
    val url: String? = null,
    @SerializedName("gmt_offset") val gmtOffset: String? = null,
    val namespaces: List<String>? = null,
    val authentication: Authentication? = null,
    val routes: Map<String, JsonElement>? = null
) : Response {
    class Authentication(
        @SerializedName("application-passwords") val applicationPasswords: ApplicationPasswords? = null
    ) : JsonObjectOrNull() {
        class ApplicationPasswords(
            val endpoints: Endpoints?
        ) {
            class Endpoints(
                val authorization: String?
            )
        }
    }
}
