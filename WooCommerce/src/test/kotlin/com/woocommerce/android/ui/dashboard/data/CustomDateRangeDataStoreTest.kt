package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import com.woocommerce.commons.stats.StatsTimeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone

class CustomDateRangeDataStoreTest {
    private lateinit var defaultTimeZone: TimeZone

    private val fakeDataStore = object : DataStore<CustomDateRange> {
        private val state = MutableStateFlow(CustomDateRange.getDefaultInstance())
        override val data: Flow<CustomDateRange> = state
        override suspend fun updateData(
            transform: suspend (CustomDateRange) -> CustomDateRange
        ): CustomDateRange {
            state.value = transform(state.value)
            return state.value
        }
    }

    private val sut = object : CustomDateRangeDataStore(fakeDataStore) {}

    @Before
    fun setUp() {
        defaultTimeZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `given a saved range, when the device timezone changes, then the same days are returned`() = runTest {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
        sut.updateDateRange(
            StatsTimeRange(
                start = dateAtStartOfDay(LocalDate.of(2026, 8, 1), "Europe/Istanbul"),
                end = dateAtStartOfDay(LocalDate.of(2026, 8, 5), "Europe/Istanbul")
            )
        )

        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val range = sut.dateRange.first()

        assertThat(range?.start?.toLocalDate("America/Los_Angeles")).isEqualTo(LocalDate.of(2026, 8, 1))
        assertThat(range?.end?.toLocalDate("America/Los_Angeles")).isEqualTo(LocalDate.of(2026, 8, 5))
    }

    private fun dateAtStartOfDay(day: LocalDate, zoneId: String): Date =
        Date.from(day.atStartOfDay(ZoneId.of(zoneId)).toInstant())

    private fun Date.toLocalDate(zoneId: String): LocalDate =
        toInstant().atZone(ZoneId.of(zoneId)).toLocalDate()
}
