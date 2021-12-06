package com.woocommerce.android.ui.analytics

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCalculator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRanges.LAST_YEAR
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange.MultipleDateRange
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionViewState.SectionDataViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.LoadingViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import org.junit.Test
import org.mockito.kotlin.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AnalyticsViewModelTest : BaseUnitTest() {
    private val dateUtil: DateUtils = mock {
        on { getCurrentDate() } doReturn ANY_DATE
        on { getCurrentDateTimeMinusDays(1) } doReturn ANY_OTHER_DATE.time
        on { getYearMonthDayStringFromDate(any()) } doReturn ANY_YEAR_VALUE
        on { getShortMonthDayAndYearString(any()) } doReturn ANY_SORT_FORMAT_VALUE
    }

    private val calculator: AnalyticsDateRangeCalculator = mock {
        on { getAnalyticsDateRangeFrom(LAST_YEAR) } doReturn MultipleDateRange(
            DateRange.SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
            DateRange.SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
        )
    }

    private val currencyFormatter: CurrencyFormatter = mock()
    private val analyticsRepository: AnalyticsRepository = mock()
    private val savedState = SavedStateHandle()

    private lateinit var sut: AnalyticsViewModel

    @Test
    fun `given an init viewState when ViewModel is created we have the expected values`() =
        testBlocking {
            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturn ANY_VALUE
                on { getString(any(), anyVararg()) } doReturn ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE
                on { getStringArray(any()) } doAnswer { dateRangeSelectors.toTypedArray() }
            }

            sut = givenAViewModel(resourceProvider)

            with(sut.state.value.analyticsDateRangeSelectorState) {
                assertNotNull(this)
                assertEquals(ANY_VALUE, selectedPeriod)
                assertEquals(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
                assertEquals(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
                assertEquals(dateRangeSelectors, availableRangeDates)
            }

            with(sut.state.value.revenueState) {
                assertTrue(this is LoadingViewState)
            }
        }

    @Test
    fun `given a view model when selected date range changes we have the expected analyticsDateViewState values`() =
        testBlocking {
            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturnConsecutively
                    listOf(ANY_VALUE, LAST_YEAR.description)
                on { getString(any(), anyVararg()) } doReturnConsecutively
                    listOf(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE)
                on { getStringArray(any()) } doAnswer { dateRangeSelectors.toTypedArray() }
            }

            sut = givenAViewModel(resourceProvider)

            sut.onSelectedDateRangeChanged(LAST_YEAR.description)

            with(sut.state.value.analyticsDateRangeSelectorState) {
                assertNotNull(this)
                assertEquals(LAST_YEAR.description, selectedPeriod)
                assertEquals(ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
                assertEquals(ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
                assertEquals(dateRangeSelectors, availableRangeDates)
            }
        }

    @Test
    fun `given a view model when selected date range changes data view state has revenueViewState values`() =
        testBlocking {
            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
                on { getString(any(), any()) } doAnswer { invMock -> invMock.arguments[0].toString() }
                on { getStringArray(any()) } doAnswer { dateRangeSelectors.toTypedArray() }
            }

            sut = givenAViewModel(resourceProvider)
            whenever(currencyFormatter.formatCurrency(TOTAL_VALUE.toString(), CURRENCY_CODE))
                .thenReturn(TOTAL_CURRENCY_VALUE)
            whenever(analyticsRepository.fetchRevenueData(any(), any()))
                .thenReturn(listOf(getRevenueStats(), getRevenueStats()).asFlow())
            whenever(currencyFormatter.formatCurrency(NET_VALUE.toString(), CURRENCY_CODE))
                .thenReturn(NET_CURRENCY_VALUE)

            sut.onSelectedDateRangeChanged(LAST_YEAR.description)

            with(sut.state.value.revenueState) {

                assertTrue(this is AnalyticsInformationViewState.DataViewState)
                assertTrue(leftSection is SectionDataViewState)
                assertEquals(TOTAL_CURRENCY_VALUE, (leftSection as SectionDataViewState).value)
                assertEquals(TOTAL_DELTA, (leftSection as SectionDataViewState).delta)

                assertTrue(rightSection is SectionDataViewState)
                assertEquals(NET_CURRENCY_VALUE, (rightSection as SectionDataViewState).value)
                assertEquals(NET_DELTA, (rightSection as SectionDataViewState).delta)
            }
        }

    private fun givenAViewModel(resourceProvider: ResourceProvider) =
        AnalyticsViewModel(resourceProvider, dateUtil, calculator, currencyFormatter, analyticsRepository, savedState)

    private fun getRevenueStats() =
        RevenueData(RevenueStat(TOTAL_VALUE, TOTAL_DELTA, NET_VALUE, NET_DELTA, CURRENCY_CODE))

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

        const val TOTAL_VALUE = 10.0
        const val TOTAL_DELTA = 5
        const val NET_VALUE = 20.0
        const val NET_DELTA = 10
        const val CURRENCY_CODE = "EUR"
        const val TOTAL_CURRENCY_VALUE = "10 E"
        const val NET_CURRENCY_VALUE = "10 E"

    }
}
