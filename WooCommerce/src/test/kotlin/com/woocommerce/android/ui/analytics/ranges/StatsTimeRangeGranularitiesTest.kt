package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.toDateAtStartOfDay
import com.woocommerce.android.util.LocalizedDatePatternsTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StatsTimeRangeGranularitiesTest {
    @get:Rule
    val localizedDatePatterns = LocalizedDatePatternsTestRule()

    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun setUp() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `given a custom range of the 25-hour DST fall-back day, when getting the revenue granularity, then it is hours`() {
        // GIVEN
        val rangeSelection = givenCustomRange(FALL_BACK_DAY, FALL_BACK_DAY)

        // WHEN
        val granularity = rangeSelection.revenueStatsGranularity

        // THEN
        assertThat(granularity).isEqualTo(StatsGranularity.HOURS)
    }

    @Test
    fun `given a custom range of 28 days including the DST fall-back day, when getting the revenue granularity, then it is days`() {
        // GIVEN
        val rangeSelection = givenCustomRange(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 11))

        // WHEN
        val granularity = rangeSelection.revenueStatsGranularity

        // THEN
        assertThat(granularity).isEqualTo(StatsGranularity.DAYS)
    }

    @Test
    fun `given a custom range of the 25-hour DST fall-back day, when getting the summary visitor granularity, then it is days`() {
        // GIVEN
        val rangeSelection = givenCustomRange(FALL_BACK_DAY, FALL_BACK_DAY)

        // WHEN
        val granularity = rangeSelection.visitorSummaryStatsGranularity

        // THEN
        assertThat(granularity).isEqualTo(StatsGranularity.DAYS)
    }

    private fun givenCustomRange(start: LocalDate, end: LocalDate) = StatsTimeRangeSelection.build(
        rangeStart = start.toEpochDay().toDateAtStartOfDay(),
        rangeEnd = end.toEpochDay().toDateAtStartOfDay(),
        calendar = Calendar.getInstance(),
        locale = Locale.US
    )

    private companion object {
        val FALL_BACK_DAY: LocalDate = LocalDate.of(2026, 11, 1)
    }
}
