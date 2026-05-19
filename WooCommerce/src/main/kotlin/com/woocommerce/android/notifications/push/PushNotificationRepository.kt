package com.woocommerce.android.notifications.push

import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.locale.LocaleProvider
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
import java.util.Locale
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
    private val notificationAnalyticsTracker: NotificationAnalyticsTracker,
    private val localeProvider: LocaleProvider,
    private val checkWooPluginPushNotificationsSupport: CheckWooPluginPushNotificationsSupport,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite
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

    suspend fun registerPushTokenInWpComSystem(token: String) {
        WooLog.d(
            tag = WooLog.T.NOTIFICATIONS,
            message = "Registering FCM token in WPCOM instance${if (BuildConfig.DEBUG) ": $token" else ""}"
        )
        wpComPushNotificationStore.registerDevice(
            token,
            WpComPushNotificationStore.NotificationAppKey.WOOCOMMERCE
        )
    }

    suspend fun registerPushTokenInWooCoreSystem(
        token: String,
        selectedSite: SiteModel,
        allowWpComFallback: Boolean = true
    ): Result<Unit> {
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
                savePushTokenForSite(selectedSite.siteId, WooPushRegistrationData(tokenId, token, deviceLocale))
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
            if (allowWpComFallback && !isWpComPushRegistered()) {
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
                errorType = error?.type?.let { it::class.simpleName },
                errorCode = when (val type = error?.type) {
                    is WpComPushNotificationStore.NotificationSettingErrorType.ApiError -> type.apiErrorCode
                    WpComPushNotificationStore.NotificationSettingErrorType.UnregisteredDevice ->
                        WPCOM_UNREGISTERED_DEVICE_ERROR_CODE

                    null -> null
                }
            )
        } else {
            WooLog.d(WooLog.T.NOTIFICATIONS, "WPCom notifications disabled for site $siteId")
            notificationAnalyticsTracker.track(
                stat = AnalyticsEvent.WPCOM_DEVICE_DISABLE_PUSH_NOTIFICATIONS_SUCCESS,
                siteId = siteId
            )
        }
    }

    private suspend fun savePushTokenForSite(siteId: Long, registration: WooPushRegistrationData) {
        pushNotificationsDataStore.edit { preferences ->
            preferences.savePushRegistration(siteId, registration)
        }
    }

    private fun Preferences.getPushRegistration(siteId: Long): WooPushRegistrationData? {
        val tokenId = this[getPushTokenIdKeyForSite(siteId)] ?: return null
        val token = this[getPushTokenValueKeyForSite(siteId)] ?: return null
        val locale = this[getPushLocaleKeyForSite(siteId)] ?: return null
        return WooPushRegistrationData(tokenId, token, locale)
    }

    private fun MutablePreferences.savePushRegistration(siteId: Long, registration: WooPushRegistrationData) {
        this[getPushTokenIdKeyForSite(siteId)] = registration.tokenId
        this[getPushTokenValueKeyForSite(siteId)] = registration.token
        this[getPushLocaleKeyForSite(siteId)] = registration.locale
    }

    private fun MutablePreferences.clearPushRegistration(siteId: Long) {
        remove(getPushTokenIdKeyForSite(siteId))
        remove(getPushTokenValueKeyForSite(siteId))
        remove(getPushLocaleKeyForSite(siteId))
    }

    private fun getPushTokenIdKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_TOKEN_KEY_PREFIX$siteId")

    private fun getPushTokenValueKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_TOKEN_VALUE_KEY_PREFIX$siteId")

    private fun getPushLocaleKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_LOCALE_KEY_PREFIX$siteId")

    suspend fun isWooPushTokenRegisteredForSite(siteId: Long): Boolean =
        observeWooPushTokenRegisteredForSite(siteId).first()

    suspend fun shouldRegisterWooPushForSite(currentToken: String, siteId: Long): Boolean {
        val preferences = pushNotificationsDataStore.data.first()
        val registration = preferences.getPushRegistration(siteId)

        return registration == null ||
            registration.token != currentToken ||
            registration.locale != getDeviceLocale()
    }

    fun isWpComPushRegistered(): Boolean =
        prefsWrapper.getFluxCPreferences()
            .getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
            .isNotNullOrEmpty()

    fun observeWooPushTokenRegisteredForSite(siteId: Long): Flow<Boolean> {
        val tokenKey = getPushTokenIdKeyForSite(siteId)
        return pushNotificationsDataStore.data.map { preferences ->
            val isTokenStored = preferences[tokenKey].isNotNullOrEmpty()
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

    suspend fun getWooPushRegisteredSiteIds(): Set<Long> {
        val preferences = pushNotificationsDataStore.data.first()
        return preferences.asMap().keys
            .mapNotNull { key ->
                key.name.removePrefix(PUSH_TOKEN_KEY_PREFIX).toLongOrNull()
            }
            .toSet()
    }

    suspend fun unregisterDeviceFromPushNotifications() {
        coroutineScope {
            val unregisterWpComToken = async {
                if (isWpComPushRegistered()) {
                    wpComPushNotificationStore.unregisterWpComPushToken()
                }
            }
            val unregisterWooCoreTokens = async { unregisterWooCoreTokensFromServer() }

            awaitAll(unregisterWpComToken, unregisterWooCoreTokens)
        }
    }

    suspend fun unregisterWooPushTokenForSite(site: SiteModel): Result<Unit> {
        val preferences = pushNotificationsDataStore.data.first()
        val registration = preferences.getPushRegistration(site.siteId) ?: return Result.success(Unit)

        val result = wooPushNotificationsStore.deletePushToken(site, registration.tokenId)
        val isAlreadyDeleted = result.error?.type == WooErrorType.INVALID_ID
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

    private data class WooPushRegistrationData(
        val tokenId: String,
        val token: String,
        val locale: String
    )

    companion object {
        private const val PUSH_TOKEN_KEY_PREFIX = "push_token_"
        private const val PUSH_TOKEN_VALUE_KEY_PREFIX = "push_token_value_"
        private const val PUSH_LOCALE_KEY_PREFIX = "push_locale_"
        private const val WPCOM_UNREGISTERED_DEVICE_ERROR_CODE = "unregistered_device"
    }
}
