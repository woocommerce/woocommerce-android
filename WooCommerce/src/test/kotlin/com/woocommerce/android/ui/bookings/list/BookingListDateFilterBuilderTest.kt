package com.woocommerce.android.ui.bookings.list

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class BookingListDateFilterBuilderTest {
    private val testZoneId = ZoneId.ofOffset("UTC", ZoneOffset.ofHours(2))
    private val mockedNow = Instant.parse("2025-01-01T12:00:00+02:00")

    private val clock = Clock.fixed(mockedNow, testZoneId)
    private val dateFilterBuilder = BookingListDateFilterBuilder(clock)

    @Test
    fun `given no user date range, when tab is Today, then returns correct date range filter`() {
        val filter = dateFilterBuilder.prepareDateFilter(
            selectedTab = BookingListTab.Today,
            selectedDateRange = BookingsFilterOption.DateRange.DEFAULT
        )

        assertThat(filter).isNotNull()
        assertThat(filter.after).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"))
        assertThat(filter.before).isEqualTo(Instant.parse("2025-01-01T23:59:59.999999999+00:00"))
    }

    @Test
    fun `given no user date range, when tab is Upcoming, then returns correct date range filter`() {
        val filter = dateFilterBuilder.prepareDateFilter(
            selectedTab = BookingListTab.Upcoming,
            selectedDateRange = BookingsFilterOption.DateRange.DEFAULT
        )

        assertThat(filter).isNotNull()
        assertThat(filter.after).isEqualTo(Instant.parse("2025-01-01T23:59:59.999999999+00:00"))
        assertThat(filter.before).isNull()
    }

    @Test
    fun `given no user date range, when tab is All, then returns default date range filter`() {
        val filter = dateFilterBuilder.prepareDateFilter(
            selectedTab = BookingListTab.All,
            selectedDateRange = BookingsFilterOption.DateRange.DEFAULT
        )

        assertThat(filter).isEqualTo(BookingsFilterOption.DateRange.DEFAULT)
    }

    @Test
    fun `given All tab with user date range, when preparing filter, then uses user date range`() {
        val userAfter = Instant.parse("2025-01-01T06:00:00Z")
        val userBefore = Instant.parse("2025-01-01T18:00:00Z")
        val userDateRange = BookingsFilterOption.DateRange(after = userAfter, before = userBefore)

        val filter = dateFilterBuilder.prepareDateFilter(
            selectedTab = BookingListTab.All,
            selectedDateRange = userDateRange
        )

        assertThat(filter.after).isEqualTo(userAfter)
        assertThat(filter.before).isEqualTo(userBefore)
    }

    @Test
    fun `given Upcoming tab and user date range with earlier 'after', when preparing filter, then uses tab 'after'`() {
        val userAfter = Instant.parse("2024-12-31T00:00:00Z")
        val userBefore = Instant.parse("2025-01-10T00:00:00Z")
        val userDateRange = BookingsFilterOption.DateRange(after = userAfter, before = userBefore)

        val filter = dateFilterBuilder.prepareDateFilter(
            selectedTab = BookingListTab.Upcoming,
            selectedDateRange = userDateRange
        )

        assertThat(filter.after).isEqualTo(Instant.parse("2025-01-01T23:59:59.999999999+00:00"))
        assertThat(filter.before).isEqualTo(userBefore)
    }

    @Test
    fun `given Today tab with user 'after' only, when preparing filter, then uses user 'after' and tab 'before'`() {
        val userAfter = Instant.parse("2025-01-01T08:00:00Z")
        val userDateRange = BookingsFilterOption.DateRange(after = userAfter, before = null)

        val filter = dateFilterBuilder.prepareDateFilter(
            selectedTab = BookingListTab.Today,
            selectedDateRange = userDateRange
        )

        assertThat(filter.after).isEqualTo(userAfter)
        assertThat(filter.before).isEqualTo(Instant.parse("2025-01-01T23:59:59.999999999+00:00"))
    }

    @Test
    fun `given Today tab with user 'before' only, when preparing filter, then uses tab 'after' and user 'before'`() {
        val userBefore = Instant.parse("2025-01-01T18:00:00Z")
        val userDateRange = BookingsFilterOption.DateRange(after = null, before = userBefore)

        val filter = dateFilterBuilder.prepareDateFilter(
            selectedTab = BookingListTab.Today,
            selectedDateRange = userDateRange
        )

        assertThat(filter.after).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"))
        assertThat(filter.before).isEqualTo(userBefore)
    }

    @Test
    fun `given Today tab with wider user range, when preparing filter, then tab range constrains 'after' and 'before'`() {
        val userAfter = Instant.parse("2024-12-31T00:00:00Z")
        val userBefore = Instant.parse("2025-01-02T00:00:00Z")
        val userDateRange = BookingsFilterOption.DateRange(after = userAfter, before = userBefore)

        val filter = dateFilterBuilder.prepareDateFilter(
            selectedTab = BookingListTab.Today,
            selectedDateRange = userDateRange
        )

        assertThat(filter.after).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"))
        assertThat(filter.before).isEqualTo(Instant.parse("2025-01-01T23:59:59.999999999+00:00"))
    }
}
