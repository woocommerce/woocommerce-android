package com.woocommerce.android.notifications.push

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object PushNotificationPreferences {
    fun Preferences.getPushRegistration(siteId: Long): WooPushRegistrationData? {
        val tokenId = this[getPushTokenIdKeyForSite(siteId)] ?: return null
        val token = this[getPushTokenValueKeyForSite(siteId)] ?: return null
        val locale = this[getPushLocaleKeyForSite(siteId)] ?: return null
        val deviceUuid = this[getPushDeviceUuidKeyForSite(siteId)]
        return WooPushRegistrationData(tokenId, token, locale, deviceUuid)
    }

    fun MutablePreferences.savePushRegistration(siteId: Long, registration: WooPushRegistrationData) {
        this[getPushTokenIdKeyForSite(siteId)] = registration.tokenId
        this[getPushTokenValueKeyForSite(siteId)] = registration.token
        this[getPushLocaleKeyForSite(siteId)] = registration.locale
        registration.deviceUuid?.let { this[getPushDeviceUuidKeyForSite(siteId)] = it }
    }

    fun MutablePreferences.clearPushRegistration(siteId: Long) {
        remove(getPushTokenIdKeyForSite(siteId))
        remove(getPushTokenValueKeyForSite(siteId))
        remove(getPushLocaleKeyForSite(siteId))
        remove(getPushDeviceUuidKeyForSite(siteId))
        remove(getRefreshedAtKeyForSite(siteId))
    }

    fun Preferences.getRegisteredSiteIds(): Set<Long> = asMap().keys
        .mapNotNull { key ->
            key.name
                .takeIf { it.startsWith(PUSH_TOKEN_KEY_PREFIX) && !it.startsWith(PUSH_TOKEN_VALUE_KEY_PREFIX) }
                ?.removePrefix(PUSH_TOKEN_KEY_PREFIX)
                ?.toLongOrNull()
        }
        .toSet()

    fun Preferences.getRefreshedAt(siteId: Long): Long? = this[getRefreshedAtKeyForSite(siteId)]

    fun MutablePreferences.saveRefreshedAt(siteId: Long, millis: Long) {
        this[getRefreshedAtKeyForSite(siteId)] = millis
    }

    fun Preferences.hasWpComPendingRestore(siteId: Long): Boolean =
        this[getWpComPendingRestoreKeyForSite(siteId)] == true

    fun MutablePreferences.setWpComPendingRestore(siteId: Long, isPending: Boolean) {
        val key = getWpComPendingRestoreKeyForSite(siteId)
        if (isPending) this[key] = true else remove(key)
    }

    private fun getPushTokenIdKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_TOKEN_KEY_PREFIX$siteId")

    private fun getPushTokenValueKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_TOKEN_VALUE_KEY_PREFIX$siteId")

    private fun getPushLocaleKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_LOCALE_KEY_PREFIX$siteId")

    private fun getPushDeviceUuidKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_DEVICE_UUID_KEY_PREFIX$siteId")

    private fun getRefreshedAtKeyForSite(siteId: Long): Preferences.Key<Long> =
        longPreferencesKey("$REFRESHED_AT_KEY_PREFIX$siteId")

    private fun getWpComPendingRestoreKeyForSite(siteId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WPCOM_PENDING_RESTORE_KEY_PREFIX$siteId")

    data class WooPushRegistrationData(
        val tokenId: String,
        val token: String,
        val locale: String,
        val deviceUuid: String?
    )

    private const val PUSH_TOKEN_KEY_PREFIX = "push_token_"
    private const val PUSH_TOKEN_VALUE_KEY_PREFIX = "push_token_value_"
    private const val PUSH_LOCALE_KEY_PREFIX = "push_locale_"
    private const val PUSH_DEVICE_UUID_KEY_PREFIX = "push_device_uuid_"
    private const val REFRESHED_AT_KEY_PREFIX = "woo_push_refreshed_at_"
    private const val WPCOM_PENDING_RESTORE_KEY_PREFIX = "woo_push_wpcom_pending_restore_"
}
