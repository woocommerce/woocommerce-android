package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing POS incremental sync timestamps using DataStore.
 * Stores and retrieves timestamps specific to the current site for products and variations sync.
 */
@Singleton
class WooPosSyncTimestampRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val dataStore: DataStore<Preferences>
) {
    private val productsTimestampKey = buildSiteSpecificKey(PRODUCTS_TIMESTAMP_KEY)
    private val variationsTimestampKey = buildSiteSpecificKey(VARIATIONS_TIMESTAMP_KEY)

    suspend fun storeProductsLastSyncTimestamp(timestampGmt: String) {
        dataStore.edit { preferences ->
            preferences[productsTimestampKey] = timestampGmt
        }
    }

    suspend fun getProductsLastSyncTimestamp(): String? {
        return dataStore.data.first()[productsTimestampKey]
    }

    suspend fun clearProductsLastSyncTimestamp() {
        dataStore.edit { preferences ->
            preferences.remove(productsTimestampKey)
        }
    }

    suspend fun storeVariationsLastSyncTimestamp(timestampGmt: String) {
        dataStore.edit { preferences ->
            preferences[variationsTimestampKey] = timestampGmt
        }
    }

    suspend fun getVariationsLastSyncTimestamp(): String? {
        return dataStore.data.first()[variationsTimestampKey]
    }

    suspend fun clearVariationsLastSyncTimestamp() {
        dataStore.edit { preferences ->
            preferences.remove(variationsTimestampKey)
        }
    }

    suspend fun clearAllSyncTimestamps() {
        dataStore.edit { preferences ->
            preferences.remove(productsTimestampKey)
            preferences.remove(variationsTimestampKey)
        }
    }

    private fun buildSiteSpecificKey(key: String): Preferences.Key<String> =
        stringPreferencesKey("${selectedSite.getOrNull()?.siteId}-$key")

    private companion object {
        const val PRODUCTS_TIMESTAMP_KEY = "pos_products_sync_timestamp"
        const val VARIATIONS_TIMESTAMP_KEY = "pos_variations_sync_timestamp"
    }
}
