package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooPosPreferencesRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val dataStore: DataStore<Preferences>
) {
    private val recentProductSearchesSiteSpecificKey = buildSiteSpecificKey(RECENT_PRODUCT_SEARCHES_KEY)
    private val recentCouponSearchesSiteSpecificKey = buildSiteSpecificKey(RECENT_COUPON_SEARCHES_KEY)
    private val wasOpenedOnceKey = booleanPreferencesKey(POS_WAS_OPENED_ONCE_KEY)
    private val allowCellularDataUpdateKey = booleanPreferencesKey(ALLOW_FULL_SYNC_ON_CELLULAR_DATA_KEY)

    val recentProductSearches: Flow<List<String>> = dataStore.data
        .map { preferences ->
            val searchesString = preferences[recentProductSearchesSiteSpecificKey] ?: ""
            if (searchesString.isEmpty()) emptyList() else searchesString.split(",")
        }

    val recentCouponSearches: Flow<List<String>> = dataStore.data
        .map { preferences ->
            val searchesString = preferences[recentCouponSearchesSiteSpecificKey] ?: ""
            if (searchesString.isEmpty()) emptyList() else searchesString.split(",")
        }

    val wasOpenedOnce: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[wasOpenedOnceKey] ?: false
        }

    val allowCellularDataUpdate: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[allowCellularDataUpdateKey] ?: true
        }

    suspend fun addRecentProductSearch(search: String) {
        dataStore.edit { preferences ->
            val currentSearches = preferences[recentProductSearchesSiteSpecificKey]?.let {
                if (it.isEmpty()) emptyList() else it.split(",")
            } ?: emptyList()

            val updatedSearches = (listOf(search) + currentSearches)
                .distinct()
                .take(MAX_RECENT_SEARCHES_COUNT)

            preferences[recentProductSearchesSiteSpecificKey] = updatedSearches.joinToString(",")
        }
    }

    suspend fun addRecentCouponSearch(search: String) {
        dataStore.edit { preferences ->
            val currentSearches = preferences[recentCouponSearchesSiteSpecificKey]?.let {
                if (it.isEmpty()) emptyList() else it.split(",")
            } ?: emptyList()

            val updatedSearches = (listOf(search) + currentSearches)
                .distinct()
                .take(MAX_RECENT_SEARCHES_COUNT)

            preferences[recentCouponSearchesSiteSpecificKey] = updatedSearches.joinToString(",")
        }
    }

    suspend fun setWasOpenedOnce(wasOpened: Boolean) {
        dataStore.edit { preferences ->
            preferences[wasOpenedOnceKey] = wasOpened
        }
    }

    suspend fun setAllowCellularDataUpdate(allow: Boolean) {
        dataStore.edit { preferences ->
            preferences[allowCellularDataUpdateKey] = allow
        }
    }

    suspend fun isPeriodicSyncEnabledForSite(siteId: Long): Boolean {
        val key = booleanPreferencesKey("pos_periodic_sync_enabled_$siteId")
        val preferences = dataStore.data.map { it[key] ?: true }.first()
        return preferences
    }

    suspend fun disablePeriodicSyncForSite(siteId: Long) {
        val key = booleanPreferencesKey("pos_periodic_sync_enabled_$siteId")
        dataStore.edit { preferences ->
            preferences[key] = false
        }
    }

    private fun buildSiteSpecificKey(key: String): Preferences.Key<String> =
        stringPreferencesKey("${selectedSite.getOrNull()?.siteId}-$key")

    private companion object {
        const val RECENT_PRODUCT_SEARCHES_KEY = "recent_product_searches_key"
        const val RECENT_COUPON_SEARCHES_KEY = "recent_coupon_searches_key"
        const val POS_WAS_OPENED_ONCE_KEY = "pos_was_opened_once_key"
        const val ALLOW_FULL_SYNC_ON_CELLULAR_DATA_KEY = "allow_full_sync_on_cellular_data_key"

        const val MAX_RECENT_SEARCHES_COUNT = 10
    }
}
