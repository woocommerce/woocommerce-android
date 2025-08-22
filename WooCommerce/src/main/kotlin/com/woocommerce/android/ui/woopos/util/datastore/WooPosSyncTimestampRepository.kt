package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosSyncTimestampRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val dataStore: DataStore<Preferences>,
    private val logger: WooPosLogWrapper
) {

    suspend fun storeProductsLastSyncTimestamp(timestampGmt: String) {
        val key = buildSiteSpecificKey(PRODUCTS_TIMESTAMP_KEY)
        if (key != null) {
            dataStore.edit { preferences ->
                preferences[key] = timestampGmt
            }
        }
    }

    suspend fun getProductsLastSyncTimestamp(): String? {
        val key = buildSiteSpecificKey(PRODUCTS_TIMESTAMP_KEY)
        return if (key != null) {
            dataStore.data.first()[key]
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

    suspend fun storeVariationsLastSyncTimestamp(timestampGmt: String) {
        val key = buildSiteSpecificKey(VARIATIONS_TIMESTAMP_KEY)
        if (key != null) {
            dataStore.edit { preferences ->
                preferences[key] = timestampGmt
            }
        }
    }

    suspend fun getVariationsLastSyncTimestamp(): String? {
        val key = buildSiteSpecificKey(VARIATIONS_TIMESTAMP_KEY)
        return if (key != null) {
            dataStore.data.first()[key]
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
        val productsKey = buildSiteSpecificKey(PRODUCTS_TIMESTAMP_KEY)
        val variationsKey = buildSiteSpecificKey(VARIATIONS_TIMESTAMP_KEY)

        if (productsKey != null && variationsKey != null) {
            dataStore.edit { preferences ->
                preferences.remove(productsKey)
                preferences.remove(variationsKey)
            }
        }
    }

    private fun buildSiteSpecificKey(key: String): Preferences.Key<String>? {
        val site = selectedSite.getOrNull()
        return if (site != null) {
            stringPreferencesKey("${site.siteId}-$key")
        } else {
            logger.e("Cannot build site-specific key '$key': no site selected")
            null
        }
    }

    private companion object {
        const val PRODUCTS_TIMESTAMP_KEY = "pos_products_sync_timestamp"
        const val VARIATIONS_TIMESTAMP_KEY = "pos_variations_sync_timestamp"
    }
}
