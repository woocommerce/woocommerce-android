package com.woocommerce.android.notifications.push

import android.content.SharedPreferences
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences

/**
 * Removes the UUID which older app versions stored in backed-up default preferences.
 *
 * The push DataStore already keeps per-site registration evidence. It remains in place so the
 * app can preserve that configuration while it registers the installation-local identity.
 */
class LegacyWooPushUuidMigration(
    private val defaultPreferences: SharedPreferences
) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        defaultPreferences.contains(LEGACY_UUID_KEY)

    override suspend fun migrate(currentData: Preferences): Preferences = currentData

    override suspend fun cleanUp() {
        check(defaultPreferences.edit().remove(LEGACY_UUID_KEY).commit()) {
            "Failed to remove migrated Woo push UUID"
        }
    }

    private companion object {
        const val LEGACY_UUID_KEY = "WOO_CORE_PUSH_DEVICE_UUID"
    }
}
