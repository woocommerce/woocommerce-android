package com.woocommerce.android.ui.analytics.hub.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_MONTH
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Calendar
import java.util.Date
import java.util.Locale

@ExperimentalCoroutinesApi
class AnalyticsUpdateDataStoreTest : BaseUnitTest() {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var currentTimeProvider: CurrentTimeProvider

    private lateinit var sut: AnalyticsUpdateDataStore

    private val defaultSelectionData = LAST_MONTH.generateSelectionData(
        calendar = Calendar.getInstance(),
        locale = Locale.getDefault(),
        referenceStartDate = Date(),
        referenceEndDate = Date()
    )

    @Test
    fun `given shouldUpdateAnalytics is called, when time elapsed is enough, then return true`() = testBlocking {
        // Given
        createAnalyticsUpdateScenarioWith(
            lastUpdateTimestamp = 1000,
            currentTimestamp = 2000
        )
        val maxOutdatedTime = 500L

        // When
        val result = sut.shouldUpdateAnalytics(
            rangeSelection = defaultSelectionData,
            analyticData = AnalyticsUpdateDataStore.AnalyticData.VISITORS,
            maxOutdatedTime = maxOutdatedTime
        ).single()

        // Then
        assertThat(result).isTrue
    }

    @Test
    fun `given shouldUpdateAnalytics is called, when time elapsed is not enough, then return false`() = testBlocking {
        // Given
        createAnalyticsUpdateScenarioWith(
            lastUpdateTimestamp = 1000,
            currentTimestamp = 1200
        )
        val maxOutdatedTime = 500L

        // When
        val result = sut.shouldUpdateAnalytics(
            rangeSelection = defaultSelectionData,
            analyticData = AnalyticsUpdateDataStore.AnalyticData.VISITORS,
            maxOutdatedTime = maxOutdatedTime
        ).single()

        // Then
        assertThat(result).isFalse
    }

    @Test
    fun `given shouldUpdateAnalytics is called, when no previous update exists, then return true`() = testBlocking {
        // Given
        createAnalyticsUpdateScenarioWith(
            lastUpdateTimestamp = null,
            currentTimestamp = 100
        )
        val maxOutdatedTime = 500L

        // When
        val result = sut.shouldUpdateAnalytics(
            rangeSelection = defaultSelectionData,
            analyticData = AnalyticsUpdateDataStore.AnalyticData.VISITORS,
            maxOutdatedTime = maxOutdatedTime
        ).single()

        // Then
        assertThat(result).isTrue
    }

    @Test
    fun `given store analytics is called with an analytic data different than ALL, then save the value for that key`() = testBlocking {
        // Given
        createAnalyticsUpdateScenarioWith(
            lastUpdateTimestamp = null,
            currentTimestamp = 100
        )
        val rangeSelection = SelectionType.TODAY.generateSelectionData(
            referenceStartDate = Date(),
            referenceEndDate = Date(),
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )

        // When
        sut.storeLastAnalyticsUpdate(
            rangeSelection = rangeSelection,
            analyticData = AnalyticsUpdateDataStore.AnalyticData.REVENUE
        )

        // Then only one value saved
        verify(dataStore).edit(any())
    }

    @Test
    fun `given store analytics is called with an analytic data list, then save the value for ALL analytic keys`() = testBlocking {
        // Given
        createAnalyticsUpdateScenarioWith(
            lastUpdateTimestamp = null,
            currentTimestamp = 100
        )
        val rangeSelection = SelectionType.TODAY.generateSelectionData(
            referenceStartDate = Date(),
            referenceEndDate = Date(),
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val analyticsData = listOf(
            AnalyticsUpdateDataStore.AnalyticData.VISITORS,
            AnalyticsUpdateDataStore.AnalyticData.BUNDLES,
            AnalyticsUpdateDataStore.AnalyticData.REVENUE
        )

        // When
        sut.storeLastAnalyticsUpdate(
            rangeSelection = rangeSelection,
            analyticDataList = analyticsData
        )

        // Then saved for all analytic data values
        verify(dataStore, times(analyticsData.size)).edit(any())
    }

    @Test
    fun `given a range selection timestamp is updated, then last update observation emits new value`() = testBlocking {
        // Given
        var timestampUpdate: Long? = null
        createAnalyticsUpdateScenarioWith(
            currentTimestamp = 2000
        )

        // When
        sut.observeLastUpdate(
            rangeSelection = defaultSelectionData,
            analyticData = AnalyticsUpdateDataStore.AnalyticData.REVENUE
        ).onEach {
            timestampUpdate = it
        }.launchIn(this)

        // Then
        assertThat(timestampUpdate).isNotNull()
        assertThat(timestampUpdate).isEqualTo(2000)
    }

    @Test
    fun `given observe should emit last update for all data sources, when a data source is missing then return null`() = testBlocking {
        // Given
        val selectedSiteId = 1
        val lastUpdateTimestamp = 2000L
        val rangeId = defaultSelectionData.selectionType.identifier
        val presentKey =
            "${selectedSiteId}${AnalyticsUpdateDataStore.AnalyticData.REVENUE}$rangeId"

        val analyticsPreferences = mock<Preferences> {
            on { get(longPreferencesKey(presentKey)) } doReturn lastUpdateTimestamp
        }

        createAnalyticsUpdateScenarioWith(analyticsPreferences, selectedSiteId)

        // When
        var timestampUpdate: Long? = null
        sut.observeLastUpdate(
            rangeSelection = defaultSelectionData,
            analyticData = listOf(
                AnalyticsUpdateDataStore.AnalyticData.REVENUE,
                AnalyticsUpdateDataStore.AnalyticData.VISITORS
            )
        ).onEach {
            timestampUpdate = it
        }.launchIn(this)

        // Then
        assertThat(timestampUpdate).isNull()
    }

    @Test
    fun `given observe should emit last update for all data sources, when all data source are present then return the oldest timestamp`() = testBlocking {
        // Given
        val selectedSiteId = 1
        val oldLastUpdateTimestamp = 2000L
        val newLastUpdateTimestamp = 2500L
        val rangeId = defaultSelectionData.selectionType.identifier
        val keyRevenue =
            "${selectedSiteId}${AnalyticsUpdateDataStore.AnalyticData.REVENUE}$rangeId"
        val keyVisitors =
            "${selectedSiteId}${AnalyticsUpdateDataStore.AnalyticData.VISITORS}$rangeId"

        val analyticsPreferences = mock<Preferences> {
            on { get(longPreferencesKey(keyRevenue)) } doReturn newLastUpdateTimestamp
            on { get(longPreferencesKey(keyVisitors)) } doReturn oldLastUpdateTimestamp
        }

        createAnalyticsUpdateScenarioWith(analyticsPreferences, selectedSiteId)

        // When
        var timestampUpdate: Long? = null
        sut.observeLastUpdate(
            rangeSelection = defaultSelectionData,
            analyticData = listOf(
                AnalyticsUpdateDataStore.AnalyticData.REVENUE,
                AnalyticsUpdateDataStore.AnalyticData.VISITORS
            )
        ).onEach {
            timestampUpdate = it
        }.launchIn(this)

        // Then
        assertThat(timestampUpdate).isNotNull()
        assertThat(timestampUpdate).isEqualTo(oldLastUpdateTimestamp)
    }

    @Test
    fun `given observe should emit last update, when all data sources are not required, if a data source is missing then return the available last update`() = testBlocking {
        // Given
        val selectedSiteId = 1
        val lastUpdateTimestamp = 2000L
        val rangeId = defaultSelectionData.selectionType.identifier
        val presentKey =
            "${selectedSiteId}${AnalyticsUpdateDataStore.AnalyticData.REVENUE}$rangeId"

        val analyticsPreferences = mock<Preferences> {
            on { get(longPreferencesKey(presentKey)) } doReturn lastUpdateTimestamp
        }

        createAnalyticsUpdateScenarioWith(analyticsPreferences, selectedSiteId)

        // When
        var timestampUpdate: Long? = null
        sut.observeLastUpdate(
            rangeSelection = defaultSelectionData,
            analyticData = listOf(
                AnalyticsUpdateDataStore.AnalyticData.REVENUE,
                AnalyticsUpdateDataStore.AnalyticData.VISITORS
            ),
            shouldAllDataBePresent = false
        ).onEach {
            timestampUpdate = it
        }.launchIn(this)

        // Then
        assertThat(timestampUpdate).isNotNull()
        assertThat(timestampUpdate).isEqualTo(lastUpdateTimestamp)
    }

    private fun createAnalyticsUpdateScenarioWith(
        analyticsPreferences: Preferences,
        selectedSiteId: Int
    ) {
        dataStore = mock {
            on { data } doReturn flowOf(analyticsPreferences)
        }

        currentTimeProvider = mock()

        val selectedSite: SelectedSite = mock {
            on { getSelectedSiteId() } doReturn selectedSiteId
        }

        sut = AnalyticsUpdateDataStore(
            dataStore = dataStore,
            currentTimeProvider = currentTimeProvider,
            selectedSite = selectedSite
        )
    }

    private fun createAnalyticsUpdateScenarioWith(
        lastUpdateTimestamp: Long?,
        currentTimestamp: Long
    ) {
        val analyticsPreferences = mock<Preferences> {
            on { get(any<Preferences.Key<Long>>()) } doReturn lastUpdateTimestamp
        }

        dataStore = mock {
            on { data } doReturn flowOf(analyticsPreferences)
        }

        val mockDate = mock<Date> {
            on { time } doReturn currentTimestamp
        }
        currentTimeProvider = mock {
            on { currentDate() } doReturn mockDate
        }

        val selectedSite: SelectedSite = mock {
            on { getSelectedSiteId() } doReturn 1
        }

        sut = AnalyticsUpdateDataStore(
            dataStore = dataStore,
            currentTimeProvider = currentTimeProvider,
            selectedSite = selectedSite
        )
    }

    private fun createAnalyticsUpdateScenarioWith(
        currentTimestamp: Long
    ) {
        val analyticsPreferences = mock<Preferences> {
            on { get(any<Preferences.Key<Long>>()) } doReturn currentTimestamp
        }

        dataStore = mock {
            on { data } doReturn flowOf(analyticsPreferences)
        }

        val selectedSite: SelectedSite = mock {
            on { getSelectedSiteId() } doReturn 1
        }

        sut = AnalyticsUpdateDataStore(
            dataStore = dataStore,
            currentTimeProvider = mock(),
            selectedSite = selectedSite
        )
    }
}
