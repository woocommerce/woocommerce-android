package com.woocommerce.android.notifications.push

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
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
        remove(getNextCheckAtKeyForSite(siteId))
    }

    fun Preferences.getRegisteredSiteIds(): Set<Long> = asMap().keys
        .mapNotNull { key ->
            key.name
                .takeIf { it.startsWith(PUSH_TOKEN_KEY_PREFIX) && !it.startsWith(PUSH_TOKEN_VALUE_KEY_PREFIX) }
                ?.removePrefix(PUSH_TOKEN_KEY_PREFIX)
                ?.toLongOrNull()
        }
        .toSet()

    fun Preferences.getNextCheckAt(siteId: Long): Long? = this[getNextCheckAtKeyForSite(siteId)]

    fun MutablePreferences.saveNextCheckAt(siteId: Long, millis: Long) {
        this[getNextCheckAtKeyForSite(siteId)] = millis
    }

    private fun getPushTokenIdKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_TOKEN_KEY_PREFIX$siteId")

    private fun getPushTokenValueKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_TOKEN_VALUE_KEY_PREFIX$siteId")

    private fun getPushLocaleKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_LOCALE_KEY_PREFIX$siteId")

    private fun getPushDeviceUuidKeyForSite(siteId: Long): Preferences.Key<String> =
        stringPreferencesKey("$PUSH_DEVICE_UUID_KEY_PREFIX$siteId")

    private fun getNextCheckAtKeyForSite(siteId: Long): Preferences.Key<Long> =
        longPreferencesKey("$NEXT_CHECK_AT_KEY_PREFIX$siteId")

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
    private const val NEXT_CHECK_AT_KEY_PREFIX = "woo_push_next_check_at_"
}
