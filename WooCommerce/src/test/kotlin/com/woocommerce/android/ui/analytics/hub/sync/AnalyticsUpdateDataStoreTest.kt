package com.woocommerce.android.ui.analytics.hub.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_MONTH
import java.util.Date
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.utils.CurrentTimeProvider

class AnalyticsUpdateDataStoreTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var currentTimeProvider: CurrentTimeProvider

    private lateinit var sut: AnalyticsUpdateDataStore

    @Before
    fun setUp() {
        val selectionData = LAST_MONTH.generateSelectionData(
            calendar = mock(),
            locale = mock()
        )
        dataStore = createDataStoreMockFor(
            rangeSelection = selectionData,
            lastUpdateTimestamp = 1000
        )

        val mockDate = mock<Date> {
            on { time } doReturn 2000
        }
        currentTimeProvider = mock {
            on { currentDate() } doReturn mockDate
        }

        sut = AnalyticsUpdateDataStore(
            dataStore = dataStore,
            currentTimeProvider = currentTimeProvider
        )
    }

    @Test
    fun `first basic test`() {
        assertThat(false).isFalse
    }

    private fun createDataStoreMockFor(
        rangeSelection: StatsTimeRangeSelection,
        lastUpdateTimestamp: Long
    ): DataStore<Preferences> {
        val analyticsPreferences = mock<Preferences> {
            on {
                get(longPreferencesKey(rangeSelection.selectionType.identifier))
            } doReturn lastUpdateTimestamp
        }
        return mock {
            on { data } doReturn flowOf(analyticsPreferences)
        }
    }
}
