package com.woocommerce.android.ui.analytics.hub.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject

/***
 * Data store responsible for signaling when the analytics data should be updated
 * through stored timestamp.
 */
class AnalyticsUpdateDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.ANALYTICS_UI_CACHE) private val dataStore: DataStore<Preferences>,
    private val currentTimeProvider: CurrentTimeProvider,
    private val selectedSite: SelectedSite
) {
    /***
     * Creates a flow that will emit true if the analytics data should be updated.
     *
     * The decision is based on the [rangeSelection] and [maxOutdatedTime] parameters.
     *
     * If no [maxOutdatedTime] is provided, the [defaultMaxOutdatedTime] value will be used.
     */
    fun shouldUpdateAnalytics(
        rangeSelection: StatsTimeRangeSelection,
        analyticData: AnalyticData,
        maxOutdatedTime: Long = defaultMaxOutdatedTime,
    ) = dataStore.data
        .map { prefs -> prefs[longPreferencesKey(getTimeStampKey(rangeSelection.identifier, analyticData))] }
        .map { lastUpdateTime -> isElapsedTimeExpired(lastUpdateTime, maxOutdatedTime) }

    fun shouldUpdateAnalytics(
        rangeSelection: StatsTimeRangeSelection,
        analyticDataList: List<AnalyticData>,
        maxOutdatedTime: Long = defaultMaxOutdatedTime,
    ): Flow<Boolean> {
        val timestampKeys = analyticDataList.map { getTimeStampKey(rangeSelection.identifier, it) }
        val flows = timestampKeys.map { timestampKey ->
            dataStore.data.map { prefs -> prefs[longPreferencesKey(timestampKey)] }
        }
        return combine(flows) { lastUpdateMillisArray ->
            lastUpdateMillisArray.all { lastUpdateTime ->
                lastUpdateTime?.let { isElapsedTimeExpired(lastUpdateTime, maxOutdatedTime) } ?: true
            }
        }
    }

    /***
     * Stores the current timestamp for a given [rangeSelection] and [analyticData]
     *
     * Will trigger a update to anyone listening to [AnalyticsUpdateDataStore.observeLastUpdate]
     */
    suspend fun storeLastAnalyticsUpdate(
        rangeSelection: StatsTimeRangeSelection,
        analyticData: AnalyticData
    ) {
        storeLastAnalyticsUpdate(getTimeStampKey(rangeSelection.identifier, analyticData))
    }

    suspend fun storeLastAnalyticsUpdate(
        rangeSelection: StatsTimeRangeSelection,
        analyticDataList: List<AnalyticData>
    ) {
        analyticDataList.forEach { analyticData ->
            storeLastAnalyticsUpdate(getTimeStampKey(rangeSelection.identifier, analyticData))
        }
    }

    /***
     * Creates a flow that will emit the latest timestamp stored for a given [rangeSelection] and [analyticData]
     *
     * Useful for views that need to always display when the analytics data was last updated.
     */
    fun observeLastUpdate(
        rangeSelection: StatsTimeRangeSelection,
        analyticData: AnalyticData
    ): Flow<Long?> {
        val timestampKeys = getTimeStampKey(rangeSelection.identifier, analyticData)
        return observeLastUpdate(timestampKeys)
    }

    /***
     * Creates a flow that will emit the latest timestamp stored for a given [rangeSelection] and list of [analyticData]
     *
     * Useful for views that need to always display when the analytics data was last updated.
     */
    fun observeLastUpdate(
        rangeSelection: StatsTimeRangeSelection,
        analyticData: List<AnalyticData>,
        shouldAllDataBePresent: Boolean = true
    ): Flow<Long?> {
        val timestampKeys = analyticData.map { data ->
            getTimeStampKey(rangeSelection.identifier, data)
        }
        return observeLastUpdate(timestampKeys, shouldAllDataBePresent)
    }

    private fun observeLastUpdate(
        timestampKeys: List<String>,
        shouldAllDataBePresent: Boolean
    ): Flow<Long?> {
        val flows = timestampKeys.map { timestampKey ->
            dataStore.data.map { prefs -> prefs[longPreferencesKey(timestampKey)] }
        }
        return combine(flows) { lastUpdateMillisArray -> lastUpdateMillisArray.filterNotNull() }
            .filter { notNullValues ->
                if (shouldAllDataBePresent) {
                    notNullValues.size == timestampKeys.size
                } else {
                    true
                }
            }
            .map { lastUpdateValues -> lastUpdateValues.minOrNull() }
    }

    private fun observeLastUpdate(timestampKey: String): Flow<Long?> {
        return dataStore.data.map { prefs -> prefs[longPreferencesKey(timestampKey)] }
    }

    private suspend fun storeLastAnalyticsUpdate(timestampKey: String) {
        dataStore.edit { preferences ->
            preferences[longPreferencesKey(timestampKey)] = currentTime
        }
    }

    private fun getTimeStampKey(identifier: String, analyticData: AnalyticData): String {
        return "${selectedSite.getSelectedSiteId()}${analyticData.name}$identifier"
    }

    private fun isElapsedTimeExpired(lastUpdateTime: Long?, maxOutdatedTime: Long): Boolean =
        lastUpdateTime?.let {
            (currentTime - it) > maxOutdatedTime
        } ?: true

    private val StatsTimeRangeSelection.identifier
        get() = if (selectionType == CUSTOM) {
            "${currentRange.start.time}-${currentRange.end.time}"
        } else {
            selectionType.identifier
        }

    private val currentTime
        get() = currentTimeProvider.currentDate().time

    companion object {
        const val defaultMaxOutdatedTime = 1000 * 60 * 30L // 30 minutes
    }

    enum class AnalyticData {
        REVENUE,
        VISITORS,
        TOP_PERFORMERS,
        ORDERS,
        BUNDLES,
        GIFT_CARDS,
        GOOGLE_ADS
    }
}

fun AnalyticsCards.toAnalyticData(): AnalyticsUpdateDataStore.AnalyticData {
    return when (this) {
        AnalyticsCards.Revenue -> AnalyticsUpdateDataStore.AnalyticData.REVENUE
        AnalyticsCards.Orders -> AnalyticsUpdateDataStore.AnalyticData.ORDERS
        AnalyticsCards.Products -> AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS
        AnalyticsCards.Session -> AnalyticsUpdateDataStore.AnalyticData.VISITORS
        AnalyticsCards.Bundles -> AnalyticsUpdateDataStore.AnalyticData.BUNDLES
        AnalyticsCards.GiftCards -> AnalyticsUpdateDataStore.AnalyticData.GIFT_CARDS
        AnalyticsCards.GoogleAds -> AnalyticsUpdateDataStore.AnalyticData.GOOGLE_ADS
    }
}
