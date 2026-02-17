package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationsRestClient @Inject constructor(private val wooNetwork: WooNetwork) {

    suspend fun registerPushToken(
        site: SiteModel,
        request: PushTokenRegistrationRequest
    ): WooPayload<PushTokenIdResponse> {
        val path = WOOCOMMERCE.push_tokens.pathPushNotifications
        val body = mutableMapOf<String, Any>(
            "token" to request.token,
            "platform" to "android",
            "origin" to request.origin,
            "device_uuid" to request.deviceUuid,
            "device_locale" to request.deviceLocale,
        )
        if (request.metadata.isNotEmpty()) {
            body["metadata"] = request.metadata
        }
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = path,
            clazz = PushTokenIdResponse::class.java,
            body = body
        ).toWooPayload()
    }

    suspend fun deletePushToken(
        site: SiteModel,
        pushTokenId: String
    ): WooPayload<Unit> = wooNetwork.executeDeleteGsonRequest(
        site = site,
        path = WOOCOMMERCE.push_tokens.id(pushTokenId.toLong()).pathPushNotifications,
        clazz = Unit::class.java,
    ).toWooPayload()

    data class PushTokenRegistrationRequest(
        val token: String,
        val origin: String,
        val deviceUuid: String,
        val deviceLocale: String,
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * @param id The unique ID of the push token record in Woo Core
     */
    data class PushTokenIdResponse(val id: String)
}
