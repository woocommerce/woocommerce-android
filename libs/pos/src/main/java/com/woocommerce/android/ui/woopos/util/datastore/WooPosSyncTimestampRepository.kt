package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WooPosSyncTimestampRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    @DataStoreQualifier(DataStoreType.WOO_POS) private val dataStore: DataStore<Preferences>,
    private val logger: WooPosLogWrapper
) {

    suspend fun storeProductsLastSyncTimestamp(timestamp: Long) {
        val key = buildSiteSpecificKey(PRODUCTS_TIMESTAMP_KEY)
        if (key != null) {
            dataStore.edit { preferences ->
                preferences[key] = timestamp.toString()
            }
        }
    }

    suspend fun getProductsLastSyncTimestamp(): Long? {
        val key = buildSiteSpecificKey(PRODUCTS_TIMESTAMP_KEY)
        return if (key != null) {
            dataStore.data.first()[key]?.toLong()
        } else {
            null
        }
    }

    suspend fun clearProductsLastSyncTimestamp() {
        val key = buildSiteSpecificKey(PRODUCTS_TIMESTAMP_KEY)
        if (key != null) {
            dataStore.edit { preferences ->
                preferences.remove(key)
            }
        }
    }

    suspend fun storeVariationsLastSyncTimestamp(timestamp: Long) {
        val key = buildSiteSpecificKey(VARIATIONS_TIMESTAMP_KEY)
        if (key != null) {
            dataStore.edit { preferences ->
                preferences[key] = timestamp.toString()
            }
        }
    }

    suspend fun getVariationsLastSyncTimestamp(): Long? {
        val key = buildSiteSpecificKey(VARIATIONS_TIMESTAMP_KEY)
        return if (key != null) {
            dataStore.data.first()[key]?.toLong()
        } else {
            null
        }
    }

    suspend fun clearVariationsLastSyncTimestamp() {
        val key = buildSiteSpecificKey(VARIATIONS_TIMESTAMP_KEY)
        if (key != null) {
            dataStore.edit { preferences ->
                preferences.remove(key)
            }
        }
    }

    suspend fun clearAllSyncTimestamps() {
        val keys = listOfNotNull(
            buildSiteSpecificKey(PRODUCTS_TIMESTAMP_KEY),
            buildSiteSpecificKey(VARIATIONS_TIMESTAMP_KEY),
            buildSiteSpecificKey(FULL_SYNC_TIMESTAMP_KEY),
            buildSiteSpecificKey(CATALOG_FILE_BLOCKED_KEY),
        )

        if (keys.isNotEmpty()) {
            dataStore.edit { preferences ->
                keys.forEach { preferences.remove(it) }
            }
        }
    }

    suspend fun storeFullSyncLastCompletedTimestamp(timestamp: Long) {
        val key = buildSiteSpecificKey(FULL_SYNC_TIMESTAMP_KEY)
        if (key != null) {
            dataStore.edit { preferences ->
                preferences[key] = timestamp.toString()
            }
        }
    }

    suspend fun getFullSyncLastCompletedTimestamp(): Long? {
        val key = buildSiteSpecificKey(FULL_SYNC_TIMESTAMP_KEY)
        return if (key != null) {
            dataStore.data.first()[key]?.toLongOrNull()
        } else {
            null
        }
    }

    suspend fun setCatalogFileBlocked(blocked: Boolean) {
        val key = buildSiteSpecificKey(CATALOG_FILE_BLOCKED_KEY)
        if (key != null) {
            dataStore.edit { preferences ->
                if (blocked) {
                    preferences[key] = true.toString()
                } else {
                    preferences.remove(key)
                }
            }
        }
    }

    suspend fun isCatalogFileBlocked(): Boolean {
        val key = buildSiteSpecificKey(CATALOG_FILE_BLOCKED_KEY)
        return if (key != null) {
            dataStore.data.first()[key]?.toBoolean() == true
        } else {
            false
        }
    }

    private fun buildSiteSpecificKey(key: String): Preferences.Key<String>? {
        val site = selectedSite.getOrNull()
        return if (site != null) {
            stringPreferencesKey("${site.remoteId().value}-$key")
        } else {
            logger.e("Cannot build site-specific key '$key': no site selected")
            null
        }
    }

    private companion object {
        const val PRODUCTS_TIMESTAMP_KEY = "pos_products_sync_timestamp"
        const val VARIATIONS_TIMESTAMP_KEY = "pos_variations_sync_timestamp"
        const val FULL_SYNC_TIMESTAMP_KEY = "pos_full_sync_completed_timestamp"
        const val CATALOG_FILE_BLOCKED_KEY = "pos_catalog_file_blocked"
    }
}
