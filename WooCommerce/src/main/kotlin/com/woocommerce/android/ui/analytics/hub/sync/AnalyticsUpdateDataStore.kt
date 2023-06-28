package com.woocommerce.android.ui.analytics.hub.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull

class AnalyticsUpdateDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.ANALYTICS) private val dataStore: DataStore<Preferences>,
    private val currentTimeInMillis: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun shouldUpdateAnalytics(rangeSelection: StatsTimeRangeSelection): Boolean {
        rangeSelection.lastUpdateTimestamp.singleOrNull()
            ?.let { currentTimeInMillis() - it }
            ?.takeIf { it < maxOutdatedTime }
            ?.let { return false }
            ?: return true
    }

    suspend fun storeLastAnalyticsUpdate(rangeSelection: StatsTimeRangeSelection) {
        dataStore.edit { preferences ->
            val timestampKey = rangeSelection.selectionType.identifier
            preferences[longPreferencesKey(timestampKey)] = currentTimeInMillis()
        }
    }

    private val StatsTimeRangeSelection.lastUpdateTimestamp
        get() = dataStore.data.map { preferences ->
            preferences[longPreferencesKey(selectionType.identifier)]
        }

    companion object {
        const val maxOutdatedTime = 1000 * 30 // 30 seconds
    }
}
