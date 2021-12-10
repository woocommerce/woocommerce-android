package com.woocommerce.android.ui.analytics

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.RefreshIndicator.*
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.LAST_YEAR
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.MultipleDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.SimpleDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCalculator
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.LoadingViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors.toList
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
            SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
            SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
        )
    }

    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(TOTAL_VALUE.toString(), CURRENCY_CODE) } doReturn TOTAL_CURRENCY_VALUE
        on { formatCurrency(NET_VALUE.toString(), CURRENCY_CODE) } doReturn NET_CURRENCY_VALUE
        on { formatCurrency(OTHER_TOTAL_VALUE.toString(), OTHER_CURRENCY_CODE) } doReturn OTHER_TOTAL_CURRENCY_VALUE
        on { formatCurrency(OTHER_NET_VALUE.toString(), OTHER_CURRENCY_CODE) } doReturn OTHER_NET_CURRENCY_VALUE
    }

    private val analyticsRepository: AnalyticsRepository = mock {
        on { getRevenueAdminPanelUrl() } doReturn ANY_URL
    }

    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { getIfExists() } doReturn siteModel
    }
    private val savedState = SavedStateHandle()

    private lateinit var sut: AnalyticsViewModel

    @Test
    fun `given an init viewState, when ViewModel is created, then has the expected values`() =
        testBlocking {
            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturn ANY_VALUE
                on { getString(any(), anyVararg()) } doReturn ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE
                on { getStringArray(any()) } doAnswer { DATE_RANGE_SELECTORS.toTypedArray() }
            }

            sut = givenAViewModel(resourceProvider)

            with(sut.state.value.analyticsDateRangeSelectorState) {
                assertNotNull(this)
                assertEquals(ANY_VALUE, selectedPeriod)
                assertEquals(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
                assertEquals(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
                assertEquals(DATE_RANGE_SELECTORS, availableRangeDates)
            }

            with(sut.state.value.revenueState) {
                assertTrue(this is LoadingViewState)
            }

            with(sut.state.value.refreshIndicator) {
                assertTrue(this is NotShowIndicator)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has the expected date range selector values`() =
        testBlocking {
            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturnConsecutively
                    listOf(ANY_VALUE, LAST_YEAR.description)
                on { getString(any(), anyVararg()) } doReturnConsecutively
                    listOf(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE)
                on { getStringArray(any()) } doAnswer { DATE_RANGE_SELECTORS.toTypedArray() }
            }

            sut = givenAViewModel(resourceProvider)
            sut.onSelectedTimePeriodChanged(LAST_YEAR.description)

            with(sut.state.value.analyticsDateRangeSelectorState) {
                assertNotNull(this)
                assertEquals(LAST_YEAR.description, selectedPeriod)
                assertEquals(ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
                assertEquals(ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
                assertEquals(DATE_RANGE_SELECTORS, availableRangeDates)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has expected revenue values`() =
        testBlocking {
            whenever(analyticsRepository.fetchRevenueData(any(), any()))
                .thenReturn(listOf(getRevenueStats(), getRevenueStats()).asFlow())

            sut = givenAViewModel()

            sut.onSelectedTimePeriodChanged(LAST_YEAR.description)

            with(sut.state.value.revenueState) {

                assertTrue(this is AnalyticsInformationViewState.DataViewState)
                assertEquals(TOTAL_CURRENCY_VALUE, leftSection.value)
                assertEquals(TOTAL_DELTA, leftSection.delta)
                assertEquals(NET_CURRENCY_VALUE, rightSection.value)
                assertEquals(NET_DELTA, rightSection.delta)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has expected refresh indicator value`() =
        testBlocking {
            whenever(analyticsRepository.fetchRevenueData(any(), any()))
                .thenReturn(listOf(getRevenueStats(), getRevenueStats()).asFlow())

            sut = givenAViewModel()

            sut.onSelectedTimePeriodChanged(LAST_YEAR.description)

            with(sut.state.value.refreshIndicator) {
                assertTrue(this is NotShowIndicator)
            }
        }

    @Test
    fun `given a WPCom site, when see report is clicked, then OpenWPComWebView event is triggered`() =
        testBlocking {
            whenever(siteModel.isWPCom).thenReturn(true)

            sut = givenAViewModel()
            sut.onRevenueSeeReportClick()

            assertThat(sut.event.value).isInstanceOf(AnalyticsViewEvent.OpenWPComWebView::class.java)
        }

    @Test
    fun `given a WPComAtomic site, when see report is clicked, then OpenWPComWebView event is triggered`() =
        testBlocking {
            whenever(siteModel.isWPComAtomic).thenReturn(true)

            sut = givenAViewModel()
            sut.onRevenueSeeReportClick()

            assertThat(sut.event.value).isInstanceOf(AnalyticsViewEvent.OpenWPComWebView::class.java)
        }

    @Test
    fun `given a no WPComAtomic and no WPCom site, when see report is clicked, then OpenUrl event is triggered`() =
        testBlocking {
            whenever(siteModel.isWPComAtomic).thenReturn(false)
            whenever(siteModel.isWPCom).thenReturn(false)

            sut = givenAViewModel()
            sut.onRevenueSeeReportClick()

            assertThat(sut.event.value).isInstanceOf(AnalyticsViewEvent.OpenUrl::class.java)
        }

    @Test
    fun `given a week to date selected, when refresh is requested, then revenue is the expected`() = testBlocking {
        val weekToDateRange = MultipleDateRange(
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
        )

        val weekRevenueStats = getRevenueStats(
            OTHER_TOTAL_VALUE,
            OTHER_TOTAL_DELTA,
            OTHER_NET_VALUE,
            OTHER_NET_DELTA,
            OTHER_CURRENCY_CODE
        )

        whenever(calculator.getAnalyticsDateRangeFrom(WEEK_TO_DATE)) doReturn weekToDateRange
        whenever(analyticsRepository.fetchRevenueData(weekToDateRange, WEEK_TO_DATE))
            .thenReturn(listOf(weekRevenueStats, weekRevenueStats).asFlow())

        sut = givenAViewModel()
        sut.onSelectedTimePeriodChanged(WEEK_TO_DATE.description)
        sut.onRefreshRequested()

        with(sut.state.value.revenueState) {
            assertTrue(this is AnalyticsInformationViewState.DataViewState)
            assertEquals(OTHER_TOTAL_CURRENCY_VALUE, leftSection.value)
            assertEquals(OTHER_TOTAL_DELTA, leftSection.delta)
            assertEquals(OTHER_NET_CURRENCY_VALUE, rightSection.value)
            assertEquals(OTHER_NET_DELTA, rightSection.delta)
        }
    }

    @Test
    fun `given a view, when refresh is requested, then show indicator is the expected`() = testBlocking {
        whenever(analyticsRepository.fetchRevenueData(any(), any()))
            .thenReturn(listOf(getRevenueStats(), getRevenueStats()).asFlow())

        val states = mutableListOf<AnalyticsViewState>()
        sut = givenAViewModel()
        sut.onRefreshRequested()
        val getShowIndicatorStatesJob = launch { sut.state.toList(states) }

        assertTrue(states[states.size - 1].refreshIndicator is NotShowIndicator)
        assertTrue(states.last().refreshIndicator is NotShowIndicator)
        getShowIndicatorStatesJob.cancel()
    }

    private fun givenAResourceProvider(): ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invMock -> invMock.arguments[0].toString() }
        on { getStringArray(any()) } doAnswer { DATE_RANGE_SELECTORS.toTypedArray() }
    }

    private fun givenAViewModel(resourceProvider: ResourceProvider = givenAResourceProvider()) =
        AnalyticsViewModel(
            resourceProvider, dateUtil, calculator,
            currencyFormatter, analyticsRepository,
            selectedSite, savedState
        )

    private fun getRevenueStats(
        totalValue: Double = TOTAL_VALUE,
        totalDelta: Int = TOTAL_DELTA,
        netValue: Double = NET_VALUE,
        netDelta: Int = NET_DELTA,
        currencyCode: String = CURRENCY_CODE
    ) = RevenueData(RevenueStat(totalValue, totalDelta, netValue, netDelta, currencyCode))

    companion object {
        private const val ANY_DATE_TIME_VALUE = "2021-11-21 00:00:00"
        private const val ANY_OTHER_DATE_TIME_VALUE = "2021-11-20 00:00:00"
        private const val ANY_WEEK_DATE_TIME_VALUE = "2010-11-20 00:00:00"

        private const val ANY_YEAR_VALUE = "2021-11-21"
        private const val ANY_SORT_FORMAT_VALUE = "21 Nov, 2021"

        private const val ANY_VALUE = "Today"
        private const val ANY_OTHER_VALUE = "Last year"

        private const val ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE = "$ANY_VALUE ($ANY_SORT_FORMAT_VALUE)"
        private const val ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE = "$ANY_OTHER_VALUE ($ANY_SORT_FORMAT_VALUE)"

        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        private val ANY_DATE: Date = sdf.parse(ANY_DATE_TIME_VALUE)!!
        private val ANY_OTHER_DATE: Date = sdf.parse(ANY_OTHER_DATE_TIME_VALUE)!!
        private val ANY_WEEK_DATE: Date = sdf.parse(ANY_WEEK_DATE_TIME_VALUE)!!
        private val DATE_RANGE_SELECTORS = listOf(ANY_VALUE, ANY_OTHER_VALUE)

        const val TOTAL_VALUE = 10.0
        const val TOTAL_DELTA = 5
        const val NET_VALUE = 20.0
        const val NET_DELTA = 10
        const val CURRENCY_CODE = "EUR"
        const val TOTAL_CURRENCY_VALUE = "10 E"
        const val NET_CURRENCY_VALUE = "10 E"

        const val OTHER_TOTAL_VALUE = 20.0
        const val OTHER_TOTAL_DELTA = 15
        const val OTHER_NET_VALUE = 10.0
        const val OTHER_NET_DELTA = 20
        const val OTHER_CURRENCY_CODE = "DOL"
        const val OTHER_TOTAL_CURRENCY_VALUE = "20 USD"
        const val OTHER_NET_CURRENCY_VALUE = "10 USD"

        const val ANY_URL = "https://a8c.com"
    }
}
