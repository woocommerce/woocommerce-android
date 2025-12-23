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
        token: String,
        origin: String,
        deviceUuid: String
    ): WooPayload<PushTokenIdResponse> {
        val path = WOOCOMMERCE.push_tokens.pathPushNotifications
        val body = mapOf(
            "token" to token,
            "platform" to "android",
            "origin" to origin,
            "device_uuid" to deviceUuid,
        )
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = path,
            clazz = PushTokenIdResponse::class.java,
            body = body
        ).toWooPayload()
    }

    suspend fun deletePushToken(
        site: SiteModel,
        pushTokenId: Int
    ): WooPayload<Unit> = wooNetwork.executeDeleteGsonRequest(
        site = site,
        path = WOOCOMMERCE.push_tokens.id(pushTokenId.toLong()).pathPushNotifications,
        clazz = Unit::class.java,
    ).toWooPayload()

    /**
     * @param id The unique ID of the push token record in Woo Core
     */
    data class PushTokenIdResponse(val id: Int)
}
