package com.woocommerce.android.ui.analytics.hub.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject

class AnalyticsUpdateDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.ANALYTICS) private val dataStore: DataStore<Preferences>,
    private val currentTimeProvider: CurrentTimeProvider
) {
    suspend fun shouldUpdateAnalytics(
        rangeSelection: StatsTimeRangeSelection,
        maxOutdatedTime: Long = defaultMaxOutdatedTime
    ) = flow {
        rangeSelection.lastUpdateTimestamp.collect { lastUpdateTimestamp ->
            lastUpdateTimestamp
                ?.let { currentTime - it }
                ?.let { timeElapsedSinceLastUpdate ->
                    emit(timeElapsedSinceLastUpdate > maxOutdatedTime)
                } ?: emit(true)
        }
    }

    suspend fun storeLastAnalyticsUpdate(rangeSelection: StatsTimeRangeSelection) {
        dataStore.edit { preferences ->
            val timestampKey = rangeSelection.selectionType.identifier
            preferences[longPreferencesKey(timestampKey)] = currentTime
        }
    }

    private val StatsTimeRangeSelection.lastUpdateTimestamp
        get() = dataStore.data.map { preferences ->
            preferences[longPreferencesKey(selectionType.identifier)]
        }

    private val currentTime
        get() = currentTimeProvider.currentDate().time

    companion object {
        const val defaultMaxOutdatedTime = 1000 * 30L // 30 seconds
    }
}
