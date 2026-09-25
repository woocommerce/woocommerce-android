package com.woocommerce.android.notifications.push

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import org.wordpress.android.fluxc.model.SiteModel

internal object PushNotificationPreferences {
    fun Preferences.getPushRegistration(siteId: Long): WooPushRegistrationData? {
        val tokenId = getPushTokenId(siteId) ?: return null
        val token = getPushTokenValue(siteId) ?: return null
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

    fun MutablePreferences.clearPushRegistration(site: SiteModel) {
        remove(getPushTokenIdKeyForSite(site.siteId))
        remove(getPushTokenValueKeyForSite(site.siteId))
        remove(getPushLocaleKeyForSite(site.siteId))
        remove(getPushDeviceUuidKeyForSite(site.siteId))
        remove(getRefreshedAtKeyForSite(site.id))
    }

    fun Preferences.getRegisteredSiteIds(): Set<Long> = asMap().keys
        .mapNotNull { key ->
            key.name
                .takeIf { it.startsWith(PUSH_TOKEN_KEY_PREFIX) && !it.startsWith(PUSH_TOKEN_VALUE_KEY_PREFIX) }
                ?.removePrefix(PUSH_TOKEN_KEY_PREFIX)
                ?.toLongOrNull()
        }
        .toSet()

    fun Preferences.getPushTokenId(siteId: Long): String? = this[getPushTokenIdKeyForSite(siteId)]

    fun Preferences.getPushTokenValue(siteId: Long): String? = this[getPushTokenValueKeyForSite(siteId)]

    fun Preferences.getRefreshedAt(localSiteId: Int): Long? = this[getRefreshedAtKeyForSite(localSiteId)]

    fun MutablePreferences.saveRefreshedAt(localSiteId: Int, millis: Long) {
        this[getRefreshedAtKeyForSite(localSiteId)] = millis
    }

    fun Preferences.hasWpComPendingRestore(localSiteId: Int): Boolean =
        this[getWpComPendingRestoreKeyForSite(localSiteId)] == true

    fun MutablePreferences.setWpComPendingRestore(localSiteId: Int, isPending: Boolean) {
        val key = getWpComPendingRestoreKeyForSite(localSiteId)
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

    private fun getRefreshedAtKeyForSite(localSiteId: Int): Preferences.Key<Long> =
        longPreferencesKey("$REFRESHED_AT_KEY_PREFIX$localSiteId")

    private fun getWpComPendingRestoreKeyForSite(localSiteId: Int): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WPCOM_PENDING_RESTORE_KEY_PREFIX$localSiteId")

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
