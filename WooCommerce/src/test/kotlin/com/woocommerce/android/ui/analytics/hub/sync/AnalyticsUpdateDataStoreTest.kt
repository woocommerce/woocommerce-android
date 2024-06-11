package com.woocommerce.android.ui.analytics.hub.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
    fun `given store analytics is called with an analytic data ALL, then save the value for ALL analytic keys`() = testBlocking {
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
        val numberOfAnalyticsDataKeys = AnalyticsUpdateDataStore.AnalyticData.entries.size

        // When
        sut.storeLastAnalyticsUpdate(
            rangeSelection = rangeSelection,
            analyticData = AnalyticsUpdateDataStore.AnalyticData.ALL
        )

        // Then saved for all analytic data values
        verify(dataStore, times(numberOfAnalyticsDataKeys)).edit(any())
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
