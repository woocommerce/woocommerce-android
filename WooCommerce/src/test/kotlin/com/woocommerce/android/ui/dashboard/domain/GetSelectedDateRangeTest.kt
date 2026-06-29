package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_WEEK
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.dashboard.data.CustomDateRangeDataStore
import com.woocommerce.android.util.CalendarHelper
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@ExperimentalCoroutinesApi
class GetSelectedDateRangeTest : BaseUnitTest() {
    private val appPrefs: AppPrefsWrapper = mock {
        on { observePrefs() } doReturn flowOf(Unit)
    }
    private val customDateRangeDataStore: CustomDateRangeDataStore = mock {
        on { dateRange } doReturn flowOf(null)
    }
    private val dateUtils: DateUtils = mock {
        on { getCurrentDateInSiteTimeZone() } doReturn date("2026-06-15")
    }
    private val calendarHelper: CalendarHelper = mock()

    @Test
    fun `given monday site week start and sunday default, when week to date is selected, then range starts monday`() =
        testBlocking {
            givenSiteCalendar(firstDayOfWeek = Calendar.MONDAY)

            val selection = createGetSelectedDateRange(WEEK_TO_DATE).invoke().first()

            assertThat(selection.currentRange.start).isEqualTo(date("2026-06-15"))
            assertThat(selection.currentRange.end).isEqualTo(endOfDay("2026-06-21"))
        }

    @Test
    fun `given sunday site week start, when week to date is selected, then range starts sunday`() = testBlocking {
        givenSiteCalendar(firstDayOfWeek = Calendar.SUNDAY)

        val selection = createGetSelectedDateRange(WEEK_TO_DATE).invoke().first()

        assertThat(selection.currentRange.start).isEqualTo(date("2026-06-14"))
        assertThat(selection.currentRange.end).isEqualTo(endOfDay("2026-06-20"))
    }

    @Test
    fun `given monday site week start and sunday default, when last week is selected, then range starts monday`() =
        testBlocking {
            givenSiteCalendar(firstDayOfWeek = Calendar.MONDAY)

            val selection = createGetSelectedDateRange(LAST_WEEK).invoke().first()

            assertThat(selection.currentRange.start).isEqualTo(date("2026-06-08"))
            assertThat(selection.currentRange.end).isEqualTo(endOfDay("2026-06-14"))
        }

    @Test
    fun `given site week start, when today is selected, then non-week output is unchanged`() = testBlocking {
        givenSiteCalendar(firstDayOfWeek = Calendar.MONDAY)

        val selection = createGetSelectedDateRange(TODAY).invoke().first()

        assertThat(selection.currentRange.start).isEqualTo(date("2026-06-15"))
        assertThat(selection.currentRange.end).isEqualTo(endOfDay("2026-06-15"))
    }

    private fun givenSiteCalendar(firstDayOfWeek: Int) {
        val calendar = Calendar.getInstance(Locale.US)
        calendar.firstDayOfWeek = firstDayOfWeek
        doReturn(calendar).`when`(calendarHelper).getCalendarForSelectedSite()
    }

    private fun createGetSelectedDateRange(selectionType: SelectionType) = object : GetSelectedDateRange(
        appPrefs = appPrefs,
        customDateRangeDataStore = customDateRangeDataStore,
        dateUtils = dateUtils,
        calendarHelper = calendarHelper
    ) {
        override fun getSelectedRange(): SelectionType = selectionType
    }

    private fun date(value: String): Date = checkNotNull(DATE_FORMAT.parse(value))

    private fun endOfDay(value: String): Date {
        return Calendar.getInstance().apply {
            time = date(value)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private companion object {
        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
