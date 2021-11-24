package com.woocommerce.android.ui.analytics


import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCalculator
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import org.junit.Test
import org.mockito.kotlin.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AnalyticsViewModelTest : BaseUnitTest() {

    companion object {

        private const val TODAY_DATE_TIME_VALUE = "2021-11-21 00:00:00"
        private const val YESTERDAY_DATE_TIME_VALUE = "2021-11-20 00:00:00"

        private const val TODAY_DATE_VALUE = "2021-11-21"
        private const val YESTERDAY_DATE_VALUE = "2021-11-20"

        private const val TODAY_SHORT_MONTH_DAY_YEAR_VALUE = "21 Nov, 2021"
        private const val YESTERDAY_SHORT_MONTH_DAY_YEAR_VALUE = "20 Nov, 2021"

        private const val TODAY = "Today"
        private const val YESTERDAY = "yesterday"

        private const val RANGE_EXPECTED_DATE_MESSAGE = "$TODAY ($TODAY_SHORT_MONTH_DAY_YEAR_VALUE)"

        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        private val TODAY_DATE: Date = sdf.parse(TODAY_DATE_TIME_VALUE)!!
        private val YESTERDAY_DATE: Date = sdf.parse(YESTERDAY_DATE_TIME_VALUE)!!

        private val dateRangeSelectors = listOf(TODAY, YESTERDAY)
    }

    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { TODAY }
        on { getString(any(), anyVararg()) } doAnswer { RANGE_EXPECTED_DATE_MESSAGE }
        on { getStringArray(any()) } doAnswer { dateRangeSelectors.toTypedArray() }
    }

    private val dateUtils: DateUtils = mock {
        on { getCurrentDate() } doReturn TODAY_DATE
        on { getCurrentDateTimeMinusDays(1) } doReturn YESTERDAY_DATE.time
        on { getYearMonthDayStringFromDate(TODAY_DATE) } doReturn TODAY_DATE_VALUE
        on { getYearMonthDayStringFromDate(YESTERDAY_DATE) } doReturn YESTERDAY_DATE_VALUE
        on { getShortMonthDayAndYearString(TODAY_DATE_VALUE) } doReturn TODAY_SHORT_MONTH_DAY_YEAR_VALUE
        on { getShortMonthDayAndYearString(YESTERDAY_DATE_VALUE) } doReturn YESTERDAY_SHORT_MONTH_DAY_YEAR_VALUE
    }

    private val analyticsDateRangeCalculator: AnalyticsDateRangeCalculator = mock()

    private val sut = AnalyticsViewModel(resourceProvider, dateUtils, analyticsDateRangeCalculator)

    @Test
    fun `analyticsDateRangeSelectorState default values are expected`() {
        with(sut.state.value?.analyticsDateRangeSelectorState) {
            assertNotNull(this)
            assertEquals(TODAY, selectedPeriod)
            assertEquals(RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
            assertEquals(RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
            assertEquals(dateRangeSelectors, availableRangeDates)
        }
    }

}
