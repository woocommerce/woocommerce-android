package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import org.wordpress.android.fluxc.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationsStore @Inject internal constructor(
    private val pushNotificationsRestClient: PushNotificationsRestClient,
    private val coroutineEngine: CoroutineEngine,
) {
    suspend fun registerPushToken(
        site: SiteModel,
        token: String,
        deviceUuid: String
    ): WooResult<Unit> = coroutineEngine.withDefaultContext(AppLog.T.API, this, "deletePushToken") {
        val origin = if (BuildConfig.DEBUG) ORIGIN_DEV else ORIGIN
        val result = pushNotificationsRestClient.registerPushToken(site, token, origin, deviceUuid)

        if (result.isError) {
            WooResult(result.error)
        } else {
            // TODO persist push token id
            WooResult(Unit)
        }
    }

    suspend fun deletePushToken(
        site: SiteModel
    ): WooResult<Unit> = coroutineEngine.withDefaultContext(AppLog.T.API, this, "deletePushToken") {
        // TODO Retrieve persisted id
        val dummyId = 0
        pushNotificationsRestClient.deletePushToken(site, dummyId).asWooResult()
    }

    companion object {
        private const val ORIGIN = "com.woocommerce.android"
        private const val ORIGIN_DEV = "com.woocommerce.android:dev"
    }
}
