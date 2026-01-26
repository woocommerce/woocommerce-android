package com.woocommerce.android.notifications.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsStore
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.SiteNotificationSetting
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.UUID
import javax.inject.Inject

class PushNotificationRepository @Inject constructor(
    private val pushNotificationsStore: PushNotificationsStore,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val notificationStore: NotificationStore,
    private val wooCommerceStore: WooCommerceStore,
    @DataStoreQualifier(DataStoreType.WOO_CORE_PUSH_NOTIFICATIONS_TOKENS)
    private val pushNotificationsDataStore: DataStore<Preferences>
) {

    suspend fun registerPushTokenInWpComSystem(token: String) {
        WooLog.d(
            tag = WooLog.T.NOTIFS,
            message = "Registering FCM token in WPCOM instance${if (BuildConfig.DEBUG) ": $token" else ""}"
        )
        notificationStore.registerDevice(token, NotificationStore.NotificationAppKey.WOOCOMMERCE)
    }

    suspend fun registerPushTokenInWooCoreSystem(token: String) {
        WooLog.d(
            tag = WooLog.T.NOTIFS,
            message = "Registering FCM token in Woo Core instance${if (BuildConfig.DEBUG) ": $token" else ""}"
        )
        selectedSite.getIfExists()?.let { site ->
            val uuid = appPrefsWrapper.wooCorePushDeviceUUID.orNullIfEmpty() ?: generateAndStoreUUID()
            val result = pushNotificationsStore.registerPushToken(
                site = site,
                token = token,
                deviceUuid = uuid
            )
            if (!result.isError) {
                result.model?.let { pushToken ->
                    savePushTokenForSite(site.siteId, pushToken)
                    disableWpComNotificationsForSite(site.siteId)
                } ?: run {
                    WooLog.w(
                        WooLog.T.NOTIFS,
                        "Push token registration in Woo Core succeeded but API returned null token;"
                    )
                }
            }
        } ?: run { WooLog.w(WooLog.T.NOTIFS, "No selected site, skipping PN registration") }
    }

    private suspend fun disableWpComNotificationsForSite(siteId: Long) {
        val setting = SiteNotificationSetting(
            siteId = siteId,
            newCommentEnabled = false,
            storeOrderEnabled = false
        )
        val result = notificationStore.updateNotificationSettingsFor(listOf(setting))
        if (result.isFailure) {
            // TODO we may want to add tracking to check how often this happens and if a fallback strategy is needed
            WooLog.w(WooLog.T.NOTIFS, "Failed to disable WPCom notifications for site $siteId")
        } else {
            WooLog.d(WooLog.T.NOTIFS, "WPCom notifications disabled for site $siteId")
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

    suspend fun getWooPushRegisteredSiteIds(): Set<Long> {
        val preferences = pushNotificationsDataStore.data.first()
        return preferences.asMap().keys
            .mapNotNull { key ->
                key.name.removePrefix(PUSH_TOKEN_KEY_PREFIX).toLongOrNull()
            }
            .toSet()
    }

    suspend fun unregisterDeviceFromAllPushes() = coroutineScope {
        val unregisterWpComToken = async { notificationStore.unregisterWpComPushToken() }
        val unregisterWooCoreTokens = async { unregisterWooCoreTokensFromServer() }

        unregisterWpComToken.await()
        unregisterWooCoreTokens.await()
    }

    private suspend fun unregisterWooCoreTokensFromServer() = coroutineScope {
        val preferences = pushNotificationsDataStore.data.first()
        val sites = wooCommerceStore.getWooCommerceSites()

        val deleteJobs = sites.mapNotNull { site ->
            val tokenKey = getPushTokenKeyForSite(site.siteId)
            val pushTokenId = preferences[tokenKey] ?: return@mapNotNull null
            async {
                val result = pushNotificationsStore.deletePushToken(site, pushTokenId)
                if (result.isError) {
                    WooLog.w(
                        WooLog.T.NOTIFS,
                        "Failed to delete push token for site ${site.siteId}: ${result.error?.message}"
                    )
                } else {
                    pushNotificationsDataStore.edit { it.remove(tokenKey) }
                    WooLog.d(WooLog.T.NOTIFS, "Woo Core push token deleted for site ${site.siteId}")
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

    companion object {
        private const val PUSH_TOKEN_KEY_PREFIX = "push_token_"
    }
}
