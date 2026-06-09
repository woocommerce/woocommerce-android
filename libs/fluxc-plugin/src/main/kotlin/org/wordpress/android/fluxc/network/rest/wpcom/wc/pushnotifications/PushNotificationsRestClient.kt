package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationsRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchNotificationPreferences(site: SiteModel): WooPayload<WooPushNotificationPreferences> {
        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = WOOCOMMERCE.preferences.pathPushNotifications,
            clazz = WooPushNotificationPreferences::class.java
        ).toWooPayload()
    }

    suspend fun updateNotificationPreferences(
        site: SiteModel,
        preferences: WooPushNotificationPreferences
    ): WooPayload<WooPushNotificationPreferences> {
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = WOOCOMMERCE.preferences.pathPushNotifications,
            clazz = WooPushNotificationPreferences::class.java,
            body = preferences.toRequestMap()
        ).toWooPayload()
    }

    suspend fun registerPushToken(
        site: SiteModel,
        request: PushTokenRegistrationRequest
    ): WooPayload<PushTokenIdResponse> {
        val path = WOOCOMMERCE.push_tokens.pathPushNotifications
        val body = mutableMapOf(
            "token" to request.token,
            "platform" to "android",
            "origin" to request.origin,
            "device_uuid" to request.deviceUuid,
            "device_locale" to request.deviceLocale,
            "metadata" to request.metadata
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
        pushTokenId: String
    ): WooPayload<Unit> = when (
        val response = wooNetwork.executeDeleteGsonRequest(
            site = site,
            path = WOOCOMMERCE.push_tokens.id(pushTokenId.toLong()).pathPushNotifications,
            clazz = Unit::class.java,
        )
    ) {
        is WPAPIResponse.Success -> WooPayload(Unit)
        is WPAPIResponse.Error -> WooPayload(response.error.toWooError())
    }

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
