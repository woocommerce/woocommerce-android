package com.woocommerce.android.ui.analytics

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCalculator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRanges
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange.MultipleDateRange
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class AnalyticsViewModelTest : BaseUnitTest() {
    companion object {
        private const val ANY_DATE_TIME_VALUE = "2021-11-21 00:00:00"
        private const val ANY_OTHER_DATE_TIME_VALUE = "2021-11-20 00:00:00"

        private const val ANY_YEAR_VALUE = "2021-11-21"
        private const val ANY_SORT_FORMAT_VALUE = "21 Nov, 2021"

        private const val ANY_VALUE = "Today"
        private const val ANY_OTHER_VALUE = "Last year"

        private const val ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE = "$ANY_VALUE ($ANY_SORT_FORMAT_VALUE)"
        private const val ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE = "$ANY_OTHER_VALUE ($ANY_SORT_FORMAT_VALUE)"

        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        private val ANY_DATE: Date = sdf.parse(ANY_DATE_TIME_VALUE)!!
        private val ANY_OTHER_DATE: Date = sdf.parse(ANY_OTHER_DATE_TIME_VALUE)!!
        private val dateRangeSelectors = listOf(ANY_VALUE, ANY_OTHER_VALUE)
    }

    private val dateUtil: DateUtils = mock {
        on { getCurrentDate() } doReturn ANY_DATE
        on { getCurrentDateTimeMinusDays(1) } doReturn ANY_OTHER_DATE.time
        on { getYearMonthDayStringFromDate(any()) } doReturn ANY_YEAR_VALUE
        on { getShortMonthDayAndYearString(any()) } doReturn ANY_SORT_FORMAT_VALUE
    }

    private val calculator: AnalyticsDateRangeCalculator = mock {
        on { getAnalyticsDateRangeFrom(AnalyticsDateRanges.LAST_YEAR) } doReturn MultipleDateRange(
            DateRange.SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
            DateRange.SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
        )
    }
    private val savedState = SavedStateHandle()

    private lateinit var sut: AnalyticsViewModel

    @Test
    fun `given an init viewState when ViewModel is created we have the expected viewState values`() = testBlocking {
        val resourceProvider: ResourceProvider = mock {
            on { getString(any()) } doReturn ANY_VALUE
            on { getString(any(), anyVararg()) } doReturn ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE
            on { getStringArray(any()) } doAnswer { dateRangeSelectors.toTypedArray() }
        }

        sut = AnalyticsViewModel(resourceProvider, dateUtil, calculator, savedState)

        with(sut.state.value.analyticsDateRangeSelectorState) {
            assertNotNull(this)
            assertEquals(ANY_VALUE, selectedPeriod)
            assertEquals(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
            assertEquals(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
            assertEquals(dateRangeSelectors, availableRangeDates)
        }
    }

    @Test
    fun `given a view model when selected date range changes we have the expected viewState values`() = testBlocking {
        val resourceProvider: ResourceProvider = mock {
            on { getString(any()) } doReturnConsecutively
                listOf(ANY_VALUE, AnalyticsDateRanges.LAST_YEAR.description)
            on { getString(any(), anyVararg()) } doReturnConsecutively
                listOf(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE)
            on { getStringArray(any()) } doAnswer { dateRangeSelectors.toTypedArray() }
        }

        sut = AnalyticsViewModel(resourceProvider, dateUtil, calculator, savedState)

        sut.onSelectedDateRangeChanged(AnalyticsDateRanges.LAST_YEAR.description)

        with(sut.state.value.analyticsDateRangeSelectorState) {
            assertNotNull(this)
            assertEquals(AnalyticsDateRanges.LAST_YEAR.description, selectedPeriod)
            assertEquals(ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
            assertEquals(ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
            assertEquals(dateRangeSelectors, availableRangeDates)
        }
    }
}
