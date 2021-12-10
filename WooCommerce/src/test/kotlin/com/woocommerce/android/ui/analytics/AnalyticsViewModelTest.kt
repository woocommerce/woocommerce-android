package com.woocommerce.android.ui.analytics

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCalculator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRanges.LAST_YEAR
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange.MultipleDateRange
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.LoadingViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(TOTAL_VALUE.toString(), CURRENCY_CODE) } doReturn TOTAL_CURRENCY_VALUE
        on { formatCurrency(NET_VALUE.toString(), CURRENCY_CODE) } doReturn NET_CURRENCY_VALUE
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
            sut.onSelectedDateRangeChanged(LAST_YEAR.description)

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

            sut.onSelectedDateRangeChanged(LAST_YEAR.description)

            with(sut.state.value.revenueState) {

                assertTrue(this is AnalyticsInformationViewState.DataViewState)
                assertEquals(TOTAL_CURRENCY_VALUE, leftSection.value)
                assertEquals(TOTAL_DELTA.toInt(), leftSection.delta)
                assertEquals(NET_CURRENCY_VALUE, rightSection.value)
                assertEquals(NET_DELTA.toInt(), rightSection.delta)
            }
        }

    @Test
    fun `given a view model with on existent delta then delta is not shown`() =
        testBlocking {
            whenever(analyticsRepository.fetchRevenueData(any(), any()))
                .thenReturn(
                    listOf(
                        getRevenueStats(
                            netDelta = DeltaPercentage.NotExist,
                            totalDelta = DeltaPercentage.NotExist
                        )
                    ).asFlow()
                )

            sut = givenAViewModel()
            sut.onSelectedDateRangeChanged(LAST_YEAR.description)

            with(sut.state.value.revenueState) {
                assertTrue(this is AnalyticsInformationViewState.DataViewState)
                assertFalse(leftSection.showDelta)
                assertFalse(rightSection.showDelta)
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
        netValue: Double = NET_VALUE,
        currencyCode: String = CURRENCY_CODE,
        totalDelta: DeltaPercentage = DeltaPercentage.Value(TOTAL_DELTA.toInt()),
        netDelta: DeltaPercentage = DeltaPercentage.Value(NET_DELTA.toInt()),
    ) = RevenueData(RevenueStat(totalValue, totalDelta, netValue, netDelta, currencyCode))

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
        private val DATE_RANGE_SELECTORS = listOf(ANY_VALUE, ANY_OTHER_VALUE)

        const val TOTAL_VALUE = 10.0
        const val TOTAL_DELTA = 5.0
        const val NET_VALUE = 20.0
        const val NET_DELTA = 10.0
        const val CURRENCY_CODE = "EUR"
        const val TOTAL_CURRENCY_VALUE = "10 E"
        const val NET_CURRENCY_VALUE = "10 E"

        const val ANY_URL = "https://a8c.com"
    }
}
