package com.woocommerce.android.notifications.push

import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType.WOO_CORE_PUSH_NOTIFICATIONS_TOKENS
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.notifications.push.PushNotificationPreferences.WooPushRegistrationData
import com.woocommerce.android.notifications.push.PushNotificationPreferences.clearPushRegistration
import com.woocommerce.android.notifications.push.PushNotificationPreferences.getNextCheckAt
import com.woocommerce.android.notifications.push.PushNotificationPreferences.getPushRegistration
import com.woocommerce.android.notifications.push.PushNotificationPreferences.getRegisteredSiteIds
import com.woocommerce.android.notifications.push.PushNotificationPreferences.saveNextCheckAt
import com.woocommerce.android.notifications.push.PushNotificationPreferences.savePushRegistration
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionTypeOrNull
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.locale.LocaleProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.WooPushNotificationsStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.SiteNotificationSetting
import org.wordpress.android.fluxc.utils.PreferenceUtils
import java.io.IOException
import java.time.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class PushNotificationRepository @Inject constructor(
    private val wooPushNotificationsStore: WooPushNotificationsStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val wpComPushNotificationStore: WpComPushNotificationStore,
    private val wooCommerceStore: WooCommerceStore,
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper,
    @DataStoreQualifier(WOO_CORE_PUSH_NOTIFICATIONS_TOKENS)
    private val pushNotificationsDataStore: DataStore<Preferences>,
    private val notificationAnalyticsTracker: NotificationAnalyticsTracker,
    private val localeProvider: LocaleProvider,
    private val checkWooPluginPushNotificationsSupport: CheckWooPluginPushNotificationsSupport,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
    private val clock: Clock
) {
    fun observeWooNotificationPreferences(): Flow<WooPushNotificationPreferences?> =
        wooPushNotificationsStore.observeNotificationPreferences(selectedSite.get())

    suspend fun fetchWooNotificationPreferences(): Result<WooPushNotificationPreferences> {
        val result = wooPushNotificationsStore.fetchNotificationPreferences(selectedSite.get())
        return if (!result.isError) {
            result.model?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Woo push notification preferences fetch succeeded but API returned null"))
        } else {
            Result.failure(WooException(result.error))
        }
    }

    suspend fun updateWooNotificationPreferences(
        preferences: WooPushNotificationPreferences
    ): Result<WooPushNotificationPreferences> {
        val result = wooPushNotificationsStore.updateNotificationPreferences(selectedSite.get(), preferences)
        return if (!result.isError) {
            result.model?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Woo push notification preferences update succeeded but API returned null"))
        } else {
            Result.failure(WooException(result.error))
        }
    }

    suspend fun registerPushTokenInWpComSystem(
        token: String
    ): WpComPushNotificationStore.RegisterDeviceResponsePayload {
        WooLog.d(
            tag = WooLog.T.NOTIFICATIONS,
            message = "Registering FCM token in WPCOM instance${if (BuildConfig.DEBUG) ": $token" else ""}"
        )
        return wpComPushNotificationStore.registerDevice(
            token,
            WpComPushNotificationStore.NotificationAppKey.WOOCOMMERCE
        )
    }

    suspend fun registerPushTokenInWooCoreSystem(token: String, selectedSite: SiteModel): Result<Unit> {
        WooLog.d(
            tag = WooLog.T.NOTIFICATIONS,
            message = "Registering FCM token in Woo Core instance${if (BuildConfig.DEBUG) ": $token" else ""}"
        )

        val uuid = appPrefsWrapper.wooCorePushDeviceUUID.orNullIfEmpty() ?: generateAndStoreUUID()
        val deviceLocale = getDeviceLocale()
        val metadata = buildDeviceMetadata()
        val result = wooPushNotificationsStore.registerPushToken(
            site = selectedSite,
            token = token,
            deviceUuid = uuid,
            deviceLocale = deviceLocale,
            metadata = metadata
        )
        return if (!result.isError) {
            result.model?.let { tokenId ->
                notificationAnalyticsTracker.track(
                    stat = AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_SUCCESS,
                    siteId = selectedSite.siteId
                )
                val wasRegistered = pushNotificationsDataStore.data.first()
                    .getPushRegistration(selectedSite.siteId) != null
                savePushTokenForSite(selectedSite, WooPushRegistrationData(tokenId, token, deviceLocale, uuid))
                // Already disabled when the store registered; repeating it on refreshes would override user settings.
                if (!wasRegistered) disableWpComNotificationsForSite(selectedSite.siteId)
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
                scheduleNextCheck(selectedSite.siteId, RETRY_INTERVAL_MILLIS)
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
                "Woo Core push token registration failed: ${result.error?.message}"
            )
            if (result.error?.type == WooErrorType.API_NOT_FOUND) {
                handleWooPushUnavailable(selectedSite)
            } else {
                scheduleNextCheck(selectedSite.siteId, RETRY_INTERVAL_MILLIS)
            }
            Result.failure(WooException(result.error))
        }
    }

    private suspend fun handleWooPushUnavailable(site: SiteModel) {
        val canUseWPCom = site.connectionTypeOrNull == SiteConnectionType.Jetpack
        // Try to restore WP.com push notifications
        val isWpComRestored = canUseWPCom && isWpComPushRegistered() &&
            enableWpComNotificationsForSites(setOf(site.siteId)).isSuccess

        pushNotificationsDataStore.edit { preferences ->
            preferences.clearPushRegistration(site.siteId)
            val interval = if (!canUseWPCom || isWpComRestored) CHECK_INTERVAL_MILLIS else RETRY_INTERVAL_MILLIS
            preferences.saveNextCheckAt(site.siteId, clock.millis() + interval)
        }
    }

    private suspend fun disableWpComNotificationsForSite(siteId: Long) {
        if (!isWpComPushRegistered()) return

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
                errorType = error?.type?.let { it::class.simpleName },
                errorCode = error.toErrorCode()
            )
        } else {
            WooLog.d(WooLog.T.NOTIFICATIONS, "WPCom notifications disabled for site $siteId")
            notificationAnalyticsTracker.track(
                stat = AnalyticsEvent.WPCOM_DEVICE_DISABLE_PUSH_NOTIFICATIONS_SUCCESS,
                siteId = siteId
            )
        }
    }

    suspend fun enableWpComNotificationsForSites(siteIds: Set<Long>): Result<Unit> {
        if (siteIds.isEmpty()) return Result.success(Unit)

        val settings = siteIds.map { siteId ->
            SiteNotificationSetting(
                siteId = siteId,
                newCommentEnabled = true,
                storeOrderEnabled = true
            )
        }
        val result = wpComPushNotificationStore.updateNotificationSettingsFor(settings)
        if (result.isFailure) {
            val error = result.exceptionOrNull() as? WpComPushNotificationStore.NotificationSettingsUpdateError
            WooLog.w(WooLog.T.NOTIFICATIONS, "Failed to enable WPCom notifications for sites $siteIds")
            siteIds.forEach { siteId ->
                notificationAnalyticsTracker.trackError(
                    stat = AnalyticsEvent.WPCOM_DEVICE_ENABLE_PUSH_NOTIFICATIONS_ERROR,
                    siteId = siteId,
                    errorDescription = error?.message,
                    errorType = error?.type?.let { it::class.simpleName },
                    errorCode = error.toErrorCode()
                )
            }
        } else {
            WooLog.d(WooLog.T.NOTIFICATIONS, "WPCom notifications enabled for sites $siteIds")
            siteIds.forEach { siteId ->
                notificationAnalyticsTracker.track(
                    stat = AnalyticsEvent.WPCOM_DEVICE_ENABLE_PUSH_NOTIFICATIONS_SUCCESS,
                    siteId = siteId
                )
            }
        }
        return result
    }

    private fun WpComPushNotificationStore.NotificationSettingsUpdateError?.toErrorCode(): String? =
        when (val type = this?.type) {
            is WpComPushNotificationStore.NotificationSettingErrorType.ApiError -> type.apiErrorCode
            WpComPushNotificationStore.NotificationSettingErrorType.UnregisteredDevice ->
                WPCOM_UNREGISTERED_DEVICE_ERROR_CODE

            null -> null
        }

    private suspend fun savePushTokenForSite(site: SiteModel, registration: WooPushRegistrationData) {
        pushNotificationsDataStore.edit { preferences ->
            preferences.savePushRegistration(site.siteId, registration)
            preferences.saveNextCheckAt(site.siteId, clock.millis() + CHECK_INTERVAL_MILLIS)
        }
    }

    private suspend fun scheduleNextCheck(siteId: Long, intervalMillis: Long) {
        pushNotificationsDataStore.edit { preferences ->
            preferences.saveNextCheckAt(siteId, clock.millis() + intervalMillis)
        }
    }

    suspend fun isWooPushTokenRegisteredForSite(siteId: Long): Boolean =
        observeWooPushTokenRegisteredForSite(siteId).first()

    suspend fun shouldRegisterWooPush(currentToken: String, site: SiteModel): Boolean {
        val preferences = pushNotificationsDataStore.data.first()
        val nextCheckAt = preferences.getNextCheckAt(site.siteId) ?: return true
        val now = clock.millis()
        if (now >= nextCheckAt || nextCheckAt > now + CHECK_INTERVAL_MILLIS) return true

        val registration = preferences.getPushRegistration(site.siteId) ?: return false
        return registration.token != currentToken ||
            registration.locale != getDeviceLocale() ||
            registration.deviceUuid != appPrefsWrapper.wooCorePushDeviceUUID
    }

    fun isWpComPushRegistered(): Boolean =
        prefsWrapper.getFluxCPreferences()
            .getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
            .isNotNullOrEmpty()

    fun observeWooPushTokenRegisteredForSite(siteId: Long): Flow<Boolean> {
        return pushNotificationsDataStore.data.map { preferences ->
            val isTokenStored = preferences.getPushRegistration(siteId)?.tokenId.isNotNullOrEmpty()
            val supportResult = checkWooPluginPushNotificationsSupport(forceRefresh = false)
            // Treat errors as "compatible" to avoid hiding entry points during temporary failures
            val isPluginCompatible = when (supportResult) {
                is CheckWooPluginPushNotificationsSupport.Result.UpdateRequired -> false
                is CheckWooPluginPushNotificationsSupport.Result.Error -> true
                else -> true
            }
            isTokenStored && isPluginCompatible
        }
    }

    suspend fun hasWooPushTokenForSite(siteId: Long): Boolean =
        pushNotificationsDataStore.data.first().getPushRegistration(siteId)?.tokenId.isNotNullOrEmpty()

    suspend fun getWooPushRegisteredSiteIds(): Set<Long> =
        pushNotificationsDataStore.data.first().getRegisteredSiteIds()

    suspend fun clearWooPushRegistrationForStaleToken(site: SiteModel, currentToken: String) {
        if (currentToken.isEmpty()) return

        var cleared = false
        pushNotificationsDataStore.edit { preferences ->
            val registration = preferences.getPushRegistration(site.siteId)
            if (registration != null && registration.token != currentToken) {
                preferences.clearPushRegistration(site.siteId)
                cleared = true
            }
        }
        if (cleared) {
            WooLog.d(WooLog.T.NOTIFICATIONS, "Cleared stale Woo Core push registration for site ${site.siteId}")
        }
    }

    suspend fun unregisterDeviceFromPushNotifications() {
        try {
            coroutineScope {
                val unregisterWpComToken = async {
                    if (isWpComPushRegistered()) {
                        wpComPushNotificationStore.unregisterWpComPushToken()
                    }
                }
                val unregisterWooCoreTokens = async { unregisterWooCoreTokensFromServer() }

                awaitAll(unregisterWpComToken, unregisterWooCoreTokens)
            }
        } finally {
            // The server delete can't be retried once the account is gone, and every application-password
            // store shares the same keys, so a leftover entry makes the next store look already registered.
            withContext(NonCancellable) { clearAllWooPushRegistrations() }
        }
    }

    private suspend fun clearAllWooPushRegistrations() {
        var clearedSiteIds: Set<Long> = emptySet()
        try {
            pushNotificationsDataStore.edit { preferences ->
                clearedSiteIds = preferences.getRegisteredSiteIds()
                preferences.clear()
            }
        } catch (e: IOException) {
            // Runs from the logout cleanup path, so it must never abort the logout or mask its error.
            WooLog.e(WooLog.T.NOTIFICATIONS, "Failed to clear local Woo push registrations at logout", e)
            return
        }
        if (clearedSiteIds.isNotEmpty()) {
            WooLog.w(
                WooLog.T.NOTIFICATIONS,
                "Cleared local Woo push registrations left behind at logout for sites $clearedSiteIds"
            )
        }
    }

    suspend fun unregisterWooPushTokenForSite(site: SiteModel): Result<Unit> {
        val preferences = pushNotificationsDataStore.data.first()
        val registration = preferences.getPushRegistration(site.siteId) ?: return Result.success(Unit)

        val result = wooPushNotificationsStore.deletePushToken(site, registration.tokenId)
        val isAlreadyDeleted = result.error?.type == WooErrorType.INVALID_ID
        if (result.isError) {
            notificationAnalyticsTracker.trackError(
                stat = AnalyticsEvent.WOO_PUSH_TOKEN_DELETE_ERROR,
                siteId = site.siteId,
                errorDescription = result.error?.message,
                errorType = result.error?.type?.name,
                errorCode = result.error?.apiErrorCode
            )
        } else {
            notificationAnalyticsTracker.track(
                stat = AnalyticsEvent.WOO_PUSH_TOKEN_DELETE_SUCCESS,
                siteId = site.siteId
            )
        }
        if (result.isError && !isAlreadyDeleted) {
            WooLog.w(
                WooLog.T.NOTIFICATIONS,
                "Failed to delete push token for site ${site.siteId}: ${result.error?.message}"
            )
            return Result.failure(WooException(result.error))
        }

        pushNotificationsDataStore.edit {
            it.clearPushRegistration(site.siteId)
        }
        if (isAlreadyDeleted) {
            WooLog.d(
                WooLog.T.NOTIFICATIONS,
                "Woo Core push token already deleted on server for site ${site.siteId}, clearing local entry"
            )
        } else {
            WooLog.d(WooLog.T.NOTIFICATIONS, "Woo Core push token deleted for site ${site.siteId}")
        }
        return Result.success(Unit)
    }

    suspend fun unregisterWooPushRegisteredSites(siteIds: Set<Long>) {
        if (siteIds.isEmpty()) return

        val sites = withContext(coroutineDispatchers.io) {
            wooCommerceStore.getWooCommerceSites()
        }.filter { it.siteId in siteIds }

        coroutineScope {
            sites.map { site -> async { unregisterWooPushTokenForSite(site) } }.awaitAll()
        }
    }

    private suspend fun unregisterWooCoreTokensFromServer() = coroutineScope {
        val sites = withContext(coroutineDispatchers.io) {
            wooCommerceStore.getWooCommerceSites()
        }

        val deleteJobs = sites.map { async { unregisterWooPushTokenForSite(it) } }

        deleteJobs.awaitAll()
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString().also {
            appPrefsWrapper.wooCorePushDeviceUUID = it
        }
    }

    private fun getDeviceLocale(): String {
        val locale = localeProvider.provideLocale() ?: Locale.getDefault()
        val language = locale.language
        val country = locale.country

        return when {
            language.isEmpty() -> "en_US"
            country.isEmpty() -> language
            else -> "${language}_$country"
        }
    }

    private fun buildDeviceMetadata(): Map<String, String> = mapOf(
        "app_version" to BuildConfig.VERSION_NAME,
        "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "os_version" to Build.VERSION.RELEASE
    )

    companion object {
        private const val WPCOM_UNREGISTERED_DEVICE_ERROR_CODE = "unregistered_device"
        private val CHECK_INTERVAL_MILLIS = 1.days.inWholeMilliseconds
        private val RETRY_INTERVAL_MILLIS = 4.hours.inWholeMilliseconds
    }
}
