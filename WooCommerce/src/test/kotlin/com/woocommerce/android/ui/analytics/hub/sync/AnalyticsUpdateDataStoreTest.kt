package com.woocommerce.android.ui.analytics.hub.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_MONTH
import com.woocommerce.android.viewmodel.BaseUnitTest
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.utils.CurrentTimeProvider

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
            lastUpdateTimestamp = 0,
            currentTimestamp = 1000
        )
        val maxOutdatedTime = 500L

        // When
        val result = sut.shouldUpdateAnalytics(
            rangeSelection = defaultSelectionData,
            maxOutdatedTime = maxOutdatedTime
        )

        // Then
        assertThat(result).isTrue
    }

    @Test
    fun `given shouldUpdateAnalytics is called, when time elapsed is not enough, then return false`() {

    }

    @Test
    fun `given shouldUpdateAnalytics is called, when no previous update exists, then return true`() {

    }

    private fun createAnalyticsUpdateScenarioWith(
        lastUpdateTimestamp: Long,
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
            currentTimeProvider = currentTimeProvider
        )
    }
}
