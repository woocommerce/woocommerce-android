package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import org.wordpress.android.fluxc.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPushNotificationsStore @Inject internal constructor(
    private val pushNotificationsRestClient: PushNotificationsRestClient,
    private val coroutineEngine: CoroutineEngine,
) {

    suspend fun registerPushToken(
        site: SiteModel,
        token: String,
        deviceUuid: String,
        deviceLocale: String,
        metadata: Map<String, String> = emptyMap()
    ): WooResult<String> =
        coroutineEngine.withDefaultContext(T.API, this, "registerWooPushToken") {
            val origin = if (BuildConfig.DEBUG) ORIGIN_DEV else ORIGIN
            val request = PushNotificationsRestClient.PushTokenRegistrationRequest(
                token = token,
                origin = origin,
                deviceUuid = deviceUuid,
                deviceLocale = deviceLocale,
                metadata = metadata
            )
            val payload = pushNotificationsRestClient.registerPushToken(site, request)

            if (payload.isError || payload.result == null) {
                WooResult(payload.error)
            } else {
                WooResult(payload.result.id)
            }
        }

    suspend fun deletePushToken(site: SiteModel, pushTokenId: String): WooResult<Unit> =
        coroutineEngine.withDefaultContext(T.API, this, "deleteWooPushToken") {
            pushNotificationsRestClient.deletePushToken(site, pushTokenId).asWooResult()
        }

    companion object {
        private const val ORIGIN = "com.woocommerce.android"
        private const val ORIGIN_DEV = "com.woocommerce.android:dev"
    }
}
