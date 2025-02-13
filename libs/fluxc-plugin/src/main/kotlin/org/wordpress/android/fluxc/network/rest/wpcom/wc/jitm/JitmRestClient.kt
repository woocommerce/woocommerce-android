package org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm

import org.wordpress.android.fluxc.generated.endpoint.JPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class JitmRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchJitmMessage(
        site: SiteModel,
        messagePath: String,
        query: String,
    ): WooPayload<Array<JITMApiResponse>> {
        val url = JPAPI.jitm.pathV4

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = mapOf(
                "message_path" to messagePath,
                "query" to query,
            ),
            clazz = Array<JITMApiResponse>::class.java
        )

        return response.toWooPayload()
    }

    suspend fun dismissJitmMessage(
        site: SiteModel,
        jitmId: String,
        featureClass: String,
    ): WooPayload<Boolean> {
        val url = JPAPI.jitm.pathV4

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf(
                "id" to jitmId,
                "feature_class" to featureClass
            ),
            clazz = Any::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                WooPayload(true)
            }
            is WPAPIResponse.Error -> {
                if (response.error.type == BaseRequest.GenericErrorType.NOT_FOUND) {
                    WooPayload(false)
                } else {
                    WooPayload(response.error.toWooError())
                }
            }
        }
    }
}
