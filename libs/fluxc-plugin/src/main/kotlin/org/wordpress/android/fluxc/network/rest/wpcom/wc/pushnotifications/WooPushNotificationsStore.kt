package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.dao.WooPushNotificationPreferencesDao
import org.wordpress.android.fluxc.persistence.entity.toEntity
import org.wordpress.android.fluxc.persistence.entity.toModel
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPushNotificationsStore @Inject internal constructor(
    private val pushNotificationsRestClient: PushNotificationsRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val preferencesDao: WooPushNotificationPreferencesDao,
) {
    fun observeNotificationPreferences(site: SiteModel): Flow<WooPushNotificationPreferences?> =
        preferencesDao.observePreferences(site.localId())
            .map { it?.toModel() }
            .distinctUntilChanged()

    suspend fun fetchNotificationPreferences(site: SiteModel): WooResult<WooPushNotificationPreferences> =
        coroutineEngine.withDefaultContext(T.API, this, "fetchWooPushNotificationPreferences") {
            pushNotificationsRestClient.fetchNotificationPreferences(site).asWooResult()
                .also { result ->
                    result.model?.let { preferencesDao.upsertPreferences(it.toEntity(site.localId())) }
                }
        }

    suspend fun updateNotificationPreferences(
        site: SiteModel,
        preferences: WooPushNotificationPreferences
    ): WooResult<WooPushNotificationPreferences> =
        coroutineEngine.withDefaultContext(T.API, this, "updateWooPushNotificationPreferences") {
            pushNotificationsRestClient.updateNotificationPreferences(site, preferences).asWooResult()
                .also { result ->
                    result.model?.let { preferencesDao.upsertPreferences(it.toEntity(site.localId())) }
                }
        }

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
