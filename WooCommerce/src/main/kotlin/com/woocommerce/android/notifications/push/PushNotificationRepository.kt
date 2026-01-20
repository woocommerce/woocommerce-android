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
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsStore
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.SiteNotificationSetting
import java.util.UUID
import javax.inject.Inject

class PushNotificationRepository @Inject constructor(
    private val pushNotificationsStore: PushNotificationsStore,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val notificationStore: NotificationStore,
    @DataStoreQualifier(DataStoreType.WOO_CORE_PUSH_NOTIFICATIONS_TOKENS)
    private val pushNotificationsDataStore: DataStore<Preferences>
) {
    suspend fun registerPushToken(token: String) {
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
                }
                disableWpComNotificationsForSite(site.siteId)
            }
        } ?: run { WooLog.w(WooLog.T.NOTIFS, "No selected site, skipping PN registration") }
    }

    private suspend fun disableWpComNotificationsForSite(siteId: Long) {
        val setting = SiteNotificationSetting(
            siteId = siteId,
            newCommentEnabled = false,
            storeOrderEnabled = false
        )
        notificationStore.updateNotificationSettingsFor(listOf(setting))
        WooLog.d(WooLog.T.NOTIFS, "WPCom notifications disabled for site $siteId")
    }

    private suspend fun savePushTokenForSite(siteId: Long, token: String) {
        pushNotificationsDataStore.edit { preferences ->
            preferences[getPushTokenKeyForSite(siteId)] = token
        }
    }

    private fun getPushTokenKeyForSite(siteId: Long): Preferences.Key<String> {
        return stringPreferencesKey("push_token_$siteId")
    }

    suspend fun unregisterDeviceFromAllPushes() = coroutineScope {
        val unregisterWpComToken = async { notificationStore.unregisterWpComPushToken() }
        val clearDataStore = async { pushNotificationsDataStore.edit { it.clear() } }

        unregisterWpComToken.await()
        WooLog.d(WooLog.T.NOTIFS, "WPCom push token unregistered")

        clearDataStore.await()
        WooLog.d(WooLog.T.NOTIFS, "Woo Core push notification tokens cleared from DataStore")
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString().also {
            appPrefsWrapper.wooCorePushDeviceUUID = it
        }
    }
}
