package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import javax.inject.Inject

class WooPosPreferencesRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    @DataStoreQualifier(DataStoreType.WOO_POS) private val dataStore: DataStore<Preferences>
) {
    private val recentProductSearchesSiteSpecificKey: Preferences.Key<String>
        get() = buildSiteSpecificKey(RECENT_PRODUCT_SEARCHES_KEY)
    private val recentCouponSearchesSiteSpecificKey: Preferences.Key<String>
        get() = buildSiteSpecificKey(RECENT_COUPON_SEARCHES_KEY)
    private val wasOpenedOnceKey = booleanPreferencesKey(POS_WAS_OPENED_ONCE_KEY)
    private val lastUsedTimestampKey = longPreferencesKey(POS_LAST_USED_TIMESTAMP_KEY)
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

    suspend fun setLastUsedTimestamp(timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[lastUsedTimestampKey] = timestamp
        }
    }

    suspend fun getLastUsedTimestamp(): Long? {
        return dataStore.data.map { preferences ->
            preferences[lastUsedTimestampKey]
        }.first()
    }

    suspend fun setAllowCellularDataUpdate(allow: Boolean) {
        dataStore.edit { preferences ->
            preferences[allowCellularDataUpdateKey] = allow
        }
    }

    suspend fun getFileBasedSyncPollAttempts(siteId: LocalOrRemoteId.LocalId): Int {
        val key = buildFileBasedSyncPollAttemptsKey(siteId)
        return dataStore.data.map { it[key] ?: 0 }.first()
    }

    suspend fun setFileBasedSyncPollAttempts(siteId: LocalOrRemoteId.LocalId, attempts: Int) {
        val key = buildFileBasedSyncPollAttemptsKey(siteId)
        dataStore.edit { preferences ->
            preferences[key] = attempts
        }
    }

    suspend fun getAndClearFileBasedSyncPollAttempts(siteId: LocalOrRemoteId.LocalId): Int {
        val key = buildFileBasedSyncPollAttemptsKey(siteId)
        var attempts = 0
        dataStore.edit { preferences ->
            attempts = preferences[key] ?: 0
            preferences.remove(key)
        }
        return attempts
    }

    private fun buildFileBasedSyncPollAttemptsKey(siteId: LocalOrRemoteId.LocalId): Preferences.Key<Int> =
        intPreferencesKey("pos_file_based_sync_poll_attempts_${siteId.value}")

    private fun buildSiteSpecificKey(key: String): Preferences.Key<String> =
        stringPreferencesKey("${selectedSite.getOrNull()?.id}_v2_$key")

    private companion object {
        const val RECENT_PRODUCT_SEARCHES_KEY = "recent_product_searches_key"
        const val RECENT_COUPON_SEARCHES_KEY = "recent_coupon_searches_key"
        const val POS_WAS_OPENED_ONCE_KEY = "pos_was_opened_once_key"
        const val POS_LAST_USED_TIMESTAMP_KEY = "pos_last_used_timestamp_key"
        const val ALLOW_FULL_SYNC_ON_CELLULAR_DATA_KEY = "allow_full_sync_on_cellular_data_key"

        const val MAX_RECENT_SEARCHES_COUNT = 10
    }
}
