package com.woocommerce.android.notifications.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType.WOO_CORE_PUSH_NOTIFICATIONS_TOKENS
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.WooPushNotificationsStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.SiteNotificationSetting
import org.wordpress.android.fluxc.utils.PreferenceUtils
import java.util.UUID
import javax.inject.Inject

class PushNotificationRepository @Inject constructor(
    private val wooPushNotificationsStore: WooPushNotificationsStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val wpComPushNotificationStore: WpComPushNotificationStore,
    private val wooCommerceStore: WooCommerceStore,
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper,
    @DataStoreQualifier(WOO_CORE_PUSH_NOTIFICATIONS_TOKENS)
    private val pushNotificationsDataStore: DataStore<Preferences>,
    private val notificationAnalyticsTracker: NotificationAnalyticsTracker
) {

    suspend fun registerPushTokenInWpComSystem(token: String) {
        WooLog.d(
            tag = WooLog.T.NOTIFICATIONS,
            message = "Registering FCM token in WPCOM instance${if (BuildConfig.DEBUG) ": $token" else ""}"
        )
        wpComPushNotificationStore.registerDevice(token, WpComPushNotificationStore.NotificationAppKey.WOOCOMMERCE)
    }

    suspend fun registerPushTokenInWooCoreSystem(token: String, selectedSite: SiteModel): Result<Unit> {
        WooLog.d(
            tag = WooLog.T.NOTIFICATIONS,
            message = "Registering FCM token in Woo Core instance${if (BuildConfig.DEBUG) ": $token" else ""}"
        )

        val uuid = appPrefsWrapper.wooCorePushDeviceUUID.orNullIfEmpty() ?: generateAndStoreUUID()
        val result = wooPushNotificationsStore.registerPushToken(
            site = selectedSite,
            token = token,
            deviceUuid = uuid
        )
        return if (!result.isError) {
            result.model?.let { pushToken ->
                notificationAnalyticsTracker.track(
                    stat = AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_SUCCESS,
                    siteId = selectedSite.siteId
                )
                savePushTokenForSite(selectedSite.siteId, pushToken)
                disableWpComNotificationsForSite(selectedSite.siteId)
                Result.success(Unit)
            } ?: run {
                val errorMsg = "Push token registration in Woo Core succeeded but API returned null token"
                WooLog.w(WooLog.T.NOTIFICATIONS, errorMsg)
                notificationAnalyticsTracker.trackError(
                    stat = AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_ERROR,
                    siteId = selectedSite.siteId,
                    errorDescription = errorMsg,
                    errorType = WooErrorType.EMPTY_RESPONSE.name
                )
                Result.failure(Exception(errorMsg))
            }
        } else {
            notificationAnalyticsTracker.trackError(
                stat = AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_ERROR,
                siteId = selectedSite.siteId,
                errorDescription = result.error?.message,
                errorType = result.error?.type?.name,
                errorCode = result.error?.apiErrorCode
            )
            WooLog.w(
                WooLog.T.NOTIFICATIONS,
                "Woo Core push token registration failed:${result.error?.message}, fallback to WPCom"
            )
            if (!isWpComPushRegistered()) {
                registerPushTokenInWpComSystem(token)
            }
            Result.failure(WooException(result.error))
        }
    }

    private suspend fun disableWpComNotificationsForSite(siteId: Long) {
        val setting = SiteNotificationSetting(
            siteId = siteId,
            newCommentEnabled = false,
            storeOrderEnabled = false
        )
        val result = wpComPushNotificationStore.updateNotificationSettingsFor(listOf(setting))
        if (result.isFailure) {
            val error = result.exceptionOrNull() as? WpComPushNotificationStore.NotificationSettingsUpdateError
            WooLog.w(WooLog.T.NOTIFICATIONS, "Failed to disable WPCom notifications for site $siteId")
            notificationAnalyticsTracker.trackError(
                stat = AnalyticsEvent.WPCOM_DEVICE_DISABLE_PUSH_NOTIFICATIONS_ERROR,
                siteId = siteId,
                errorDescription = error?.message,
                errorType = error?.type?.let { it::class.simpleName }
            )
        } else {
            WooLog.d(WooLog.T.NOTIFICATIONS, "WPCom notifications disabled for site $siteId")
            notificationAnalyticsTracker.track(
                stat = AnalyticsEvent.WPCOM_DEVICE_DISABLE_PUSH_NOTIFICATIONS_SUCCESS,
                siteId = siteId
            )
        }
    }

    private suspend fun savePushTokenForSite(siteId: Long, token: String) {
        pushNotificationsDataStore.edit { preferences ->
            preferences[getPushTokenKeyForSite(siteId)] = token
        }
    }

    private fun getPushTokenKeyForSite(siteId: Long): Preferences.Key<String> {
        return stringPreferencesKey("$PUSH_TOKEN_KEY_PREFIX$siteId")
    }

    suspend fun isWooPushTokenRegisteredForSite(siteId: Long): Boolean {
        val preferences = pushNotificationsDataStore.data.first()
        val tokenKey = getPushTokenKeyForSite(siteId)
        return preferences[tokenKey] != null
    }

    fun observeWooPushTokenRegisteredForSite(siteId: Long): Flow<Boolean> {
        val tokenKey = getPushTokenKeyForSite(siteId)
        return pushNotificationsDataStore.data.map { preferences -> preferences[tokenKey] != null }
    }

    suspend fun getWooPushRegisteredSiteIds(): Set<Long> {
        val preferences = pushNotificationsDataStore.data.first()
        return preferences.asMap().keys
            .mapNotNull { key ->
                key.name.removePrefix(PUSH_TOKEN_KEY_PREFIX).toLongOrNull()
            }
            .toSet()
    }

    suspend fun unregisterDeviceFromAllPushes() = coroutineScope {
        val unregisterWpComToken = async { wpComPushNotificationStore.unregisterWpComPushToken() }
        val unregisterWooCoreTokens = async { unregisterWooCoreTokensFromServer() }

        unregisterWpComToken.await()
        unregisterWooCoreTokens.await()
    }

    suspend fun unregisterWooCoreTokensFromServer() = coroutineScope {
        val preferences = pushNotificationsDataStore.data.first()
        val sites = wooCommerceStore.getWooCommerceSites()

        val deleteJobs = sites.mapNotNull { site ->
            val tokenKey = getPushTokenKeyForSite(site.siteId)
            val pushTokenId = preferences[tokenKey] ?: return@mapNotNull null
            async {
                val result = wooPushNotificationsStore.deletePushToken(site, pushTokenId)
                if (result.isError) {
                    WooLog.w(
                        WooLog.T.NOTIFICATIONS,
                        "Failed to delete push token for site ${site.siteId}: ${result.error?.message}"
                    )
                } else {
                    pushNotificationsDataStore.edit { it.remove(tokenKey) }
                    WooLog.d(WooLog.T.NOTIFICATIONS, "Woo Core push token deleted for site ${site.siteId}")
                }
            }
        }

        deleteJobs.awaitAll()
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString().also {
            appPrefsWrapper.wooCorePushDeviceUUID = it
        }
    }

    private fun isWpComPushRegistered(): Boolean {
        val wpComPushServerId = prefsWrapper.getFluxCPreferences()
            .getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
        return wpComPushServerId.isNotNullOrEmpty()
    }

    companion object {
        private const val PUSH_TOKEN_KEY_PREFIX = "push_token_"
    }
}
