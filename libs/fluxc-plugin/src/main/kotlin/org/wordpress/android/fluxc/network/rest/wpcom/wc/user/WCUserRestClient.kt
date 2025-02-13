package org.wordpress.android.fluxc.network.rest.wpcom.wc.user

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class WCUserRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchUserInfo(
        site: SiteModel
    ): WooPayload<UserApiResponse> {
        val url = WPAPI.users.me.urlV2
        val params = mapOf(
            "context" to "edit"
        )

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = UserApiResponse::class.java
        ).toWooPayload()
    }

    data class UserApiResponse(
        val id: Long,
        val username: String?,
        val name: String?,
        @SerializedName("first_name") val firstName: String?,
        @SerializedName("last_name") val lastName: String?,
        val email: String?,
        val roles: JsonElement
    )
}
