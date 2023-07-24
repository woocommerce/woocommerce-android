package com.woocommerce.android.ui.analytics.hub.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_MONTH
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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
        locale = Locale.getDefault()
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

    private fun createAnalyticsUpdateScenarioWith(
        lastUpdateTimestamp: Long?,
        currentTimestamp: Long
    ) {
        val analyticsPreferences = mock<Preferences> {
            on {
                get(longPreferencesKey(defaultSelectionData.selectionType.identifier))
            } doReturn lastUpdateTimestamp
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

        sut = AnalyticsUpdateDataStore(
            dataStore = dataStore,
            currentTimeProvider = currentTimeProvider,
            mock()
        )
    }
}
