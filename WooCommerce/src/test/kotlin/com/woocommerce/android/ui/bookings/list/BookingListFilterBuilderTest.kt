package com.woocommerce.android.ui.bookings.list

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class BookingListFilterBuilderTest {
    private val testZoneId = ZoneId.ofOffset("UTC", ZoneOffset.ofHours(2))
    private val mockedNow = Instant.parse("2025-01-01T12:00:00+02:00")

    private val clock = Clock.fixed(mockedNow, testZoneId)
    private val filterBuilder = BookingListFiltersBuilder(clock)

    @Test
    fun `when tab is Today, then returns correct date range filter`() {
        val filter = with(filterBuilder) {
            BookingListTab.Today.asDateRangeFilter()
        }

        assertThat(filter).isNotNull()
        assertThat(filter?.after).isEqualTo(Instant.parse("2024-12-31T23:59:59.999999999Z"))
        assertThat(filter?.before).isEqualTo(Instant.parse("2025-01-01T23:59:59.999999999+00:00"))
    }

    @Test
    fun `when tab is Upcoming, then returns correct date range filter`() {
        val filter = with(filterBuilder) {
            BookingListTab.Upcoming.asDateRangeFilter()
        }

        assertThat(filter).isNotNull()
        assertThat(filter?.after).isEqualTo(Instant.parse("2025-01-01T23:59:59.999999999+00:00"))
        assertThat(filter?.before).isNull()
    }

    @Test
    fun `when tab is All, then returns null date range filter`() {
        val filter = with(filterBuilder) {
            BookingListTab.All.asDateRangeFilter()
        }

        assertThat(filter).isNull()
    }
}
