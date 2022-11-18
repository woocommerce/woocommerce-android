package com.woocommerce.android.ui.analytics

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductItem
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.VisitorsStat
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.ForceNew
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.Saved
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.ProductsResult.ProductsData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.ProductsResult.ProductsError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.VisitorsResult.VisitorsData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.VisitorsResult.VisitorsError
import com.woocommerce.android.ui.analytics.AnalyticsViewModel.Companion.DATE_RANGE_SELECTED_KEY
import com.woocommerce.android.ui.analytics.AnalyticsViewModel.Companion.TIME_PERIOD_SELECTED_KEY
import com.woocommerce.android.ui.analytics.RefreshIndicator.NotShowIndicator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.CUSTOM
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.LAST_QUARTER
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.LAST_YEAR
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.QUARTER_TO_DATE
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.TODAY
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCalculator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeFormatter
import com.woocommerce.android.ui.analytics.daterangeselector.MultipleDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.SimpleDateRange
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.LoadingViewState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AnalyticsViewModelTest : BaseUnitTest() {
    private val dateUtil: DateUtils = mock {
        on { getYearMonthDayStringFromDate(any()) } doReturn ANY_YEAR_VALUE
        on { getShortMonthDayAndYearString(any()) } doReturn ANY_SORT_FORMAT_VALUE
    }

    private val calculator: AnalyticsDateRangeCalculator = mock {
        on { getAnalyticsDateRangeFrom(LAST_YEAR) } doReturn MultipleDateRange(
            SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
            SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
        )

        on { getAnalyticsDateRangeFrom(TODAY) } doReturn
            SimpleDateRange(ANY_DATE, ANY_OTHER_DATE)
    }

    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(TOTAL_VALUE.toString(), CURRENCY_CODE) } doReturn TOTAL_CURRENCY_VALUE
        on { formatCurrency(NET_VALUE.toString(), CURRENCY_CODE) } doReturn NET_CURRENCY_VALUE
        on { formatCurrency(OTHER_TOTAL_VALUE.toString(), OTHER_CURRENCY_CODE) } doReturn OTHER_TOTAL_CURRENCY_VALUE
        on { formatCurrency(OTHER_NET_VALUE.toString(), OTHER_CURRENCY_CODE) } doReturn OTHER_NET_CURRENCY_VALUE
        on { formatCurrency(AVG_ORDER_VALUE.toString(), CURRENCY_CODE) } doReturn AVG_CURRENCY_VALUE
        on { formatCurrency(OTHER_AVG_ORDER_VALUE.toString(), OTHER_CURRENCY_CODE) } doReturn OTHER_AVG_CURRENCY_VALUE
        on { formatCurrency(PRODUCT_NET_SALES.toString(), CURRENCY_CODE) } doReturn PRODUCT_CURRENCY_VALUE
        on { formatCurrency(OTHER_PRODUCT_NET_SALES.toString(), OTHER_CURRENCY_CODE) } doReturn
            OTHER_PRODUCT_CURRENCY_VALUE
    }

    private val analyticsRepository: AnalyticsRepository = mock()

    private val savedState = AnalyticsFragmentArgs(targetGranularity = TODAY).initSavedStateHandle()

    private val transactionLauncher = mock<AnalyticsHubTransactionLauncher>()

    private lateinit var sut: AnalyticsViewModel

    @Test
    fun `given an init viewState, when view model is created, then has the expected values`() =
        testBlocking {
            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturn ANY_VALUE
                on { getString(any(), anyVararg()) } doReturn ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE
                on { getStringArray(any()) } doAnswer { DATE_RANGE_SELECTORS.toTypedArray() }
            }

            sut = givenAViewModel(resourceProvider)

            with(sut.state.value.analyticsDateRangeSelectorState) {
                assertEquals(ANY_VALUE, selectedPeriod)
                assertEquals(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
                assertEquals(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
                assertEquals(DATE_RANGE_SELECTORS, availableRangeDates)
            }

            with(sut.state.value.revenueState) {
                assertTrue(this is LoadingViewState)
            }

            with(sut.state.value.ordersState) {
                assertTrue(this is LoadingViewState)
            }

            with(sut.state.value.productsState) {
                assertTrue(this is AnalyticsListViewState.LoadingViewState)
            }

            with(sut.state.value.refreshIndicator) {
                assertTrue(this is NotShowIndicator)
            }
        }

    @Test
    fun `when ViewModel is with savedState is created, then has the expected values`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchRevenueData(any(), any(), eq(Saved)) }.doReturn(getRevenueStats())
                onBlocking { fetchOrdersData(any(), any(), eq(Saved)) }.doReturn(getOrdersStats())
            }

            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturn ANY_SAVED_VALUE
                on { getString(any(), anyVararg()) } doReturn ANY_SAVED_RANGE_EXPECTED_DATE_MESSAGE
                on { getStringArray(any()) } doAnswer { DATE_RANGE_SELECTORS.toTypedArray() }
            }

            savedState.set(TIME_PERIOD_SELECTED_KEY, LAST_YEAR)
            savedState.set(
                DATE_RANGE_SELECTED_KEY,
                MultipleDateRange(
                    SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
                    SimpleDateRange(ANY_OTHER_DATE, ANY_OTHER_DATE),
                )
            )

            sut = givenAViewModel(resourceProvider)

            with(sut.state.value.analyticsDateRangeSelectorState) {
                assertEquals(ANY_SAVED_VALUE, selectedPeriod)
                assertEquals(ANY_SAVED_RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
                assertEquals(ANY_SAVED_RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
                assertEquals(DATE_RANGE_SELECTORS, availableRangeDates)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has the expected date range selector values`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchRevenueData(any(), any(), eq(Saved)) }.doReturn(getRevenueStats())
                onBlocking { fetchOrdersData(any(), any(), eq(Saved)) }.doReturn(getOrdersStats())
            }

            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturnConsecutively
                    listOf(ANY_VALUE, LAST_YEAR.description)
                on { getString(any(), anyVararg()) } doReturnConsecutively
                    listOf(ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE, ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE)
                on { getStringArray(any()) } doAnswer { DATE_RANGE_SELECTORS.toTypedArray() }
            }

            sut = givenAViewModel(resourceProvider)
            sut.onSelectedTimePeriodChanged(LAST_YEAR)

            with(sut.state.value.analyticsDateRangeSelectorState) {
                assertEquals(LAST_YEAR.description, selectedPeriod)
                assertEquals(ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE, fromDatePeriod)
                assertEquals(ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE, toDatePeriod)
                assertEquals(DATE_RANGE_SELECTORS, availableRangeDates)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has expected revenue values`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchRevenueData(any(), any(), eq(Saved)) }.doReturn(getRevenueStats())
            }

            sut = givenAViewModel()

            sut.onSelectedTimePeriodChanged(LAST_YEAR)

            val resourceProvider = givenAResourceProvider()
            with(sut.state.value.revenueState) {
                assertTrue(this is AnalyticsInformationViewState.DataViewState)
                assertEquals(resourceProvider.getString(R.string.analytics_revenue_card_title), title)
                assertEquals(resourceProvider.getString(R.string.analytics_total_sales_title), leftSection.title)
                assertEquals(TOTAL_CURRENCY_VALUE, leftSection.value)
                assertEquals(resourceProvider.getString(R.string.analytics_net_sales_title), rightSection.title)
                assertEquals(TOTAL_DELTA.toInt(), leftSection.delta)
                assertEquals(NET_CURRENCY_VALUE, rightSection.value)
                assertEquals(NET_DELTA.toInt(), rightSection.delta)
            }
        }

    @Test
    fun `given a view model with on existent delta then delta is not shown`() =
        testBlocking {
            whenever(analyticsRepository.fetchRevenueData(any(), any(), eq(Saved)))
                .thenReturn(
                    getRevenueStats(
                        netDelta = DeltaPercentage.NotExist,
                        totalDelta = DeltaPercentage.NotExist
                    )
                )

            sut = givenAViewModel()
            sut.onSelectedTimePeriodChanged(LAST_YEAR)

            with(sut.state.value.revenueState) {
                assertTrue(this is AnalyticsInformationViewState.DataViewState)
                assertTrue(leftSection.delta == null)
                assertTrue(rightSection.delta == null)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has expected refresh indicator value`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchRevenueData(any(), any(), eq(Saved)) }.doReturn(getRevenueStats())
            }

            sut = givenAViewModel()

            sut.onSelectedTimePeriodChanged(LAST_YEAR)

            with(sut.state.value.refreshIndicator) {
                assertTrue(this is NotShowIndicator)
            }
        }

    @Test
    fun `given a week to date selected, when refresh is requested, then has expected revenue values`() = testBlocking {
        val weekToDateRange = MultipleDateRange(
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
        )

        val weekRevenueStats = getRevenueStats(
            OTHER_TOTAL_VALUE,
            OTHER_NET_VALUE,
            OTHER_CURRENCY_CODE,
            DeltaPercentage.Value(OTHER_TOTAL_DELTA),
            DeltaPercentage.Value(OTHER_NET_DELTA),
        )

        whenever(calculator.getAnalyticsDateRangeFrom(WEEK_TO_DATE)) doReturn weekToDateRange
        analyticsRepository.stub {
            onBlocking { fetchRevenueData(weekToDateRange, WEEK_TO_DATE, ForceNew) }.doReturn(weekRevenueStats)
        }

        sut = givenAViewModel()
        sut.onSelectedTimePeriodChanged(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val resourceProvider = givenAResourceProvider()
        with(sut.state.value.revenueState) {
            assertTrue(this is AnalyticsInformationViewState.DataViewState)
            assertEquals(resourceProvider.getString(R.string.analytics_revenue_card_title), title)
            assertEquals(resourceProvider.getString(R.string.analytics_total_sales_title), leftSection.title)
            assertEquals(OTHER_TOTAL_CURRENCY_VALUE, leftSection.value)
            assertEquals(OTHER_TOTAL_DELTA, leftSection.delta)
            assertEquals(resourceProvider.getString(R.string.analytics_net_sales_title), rightSection.title)
            assertEquals(OTHER_NET_CURRENCY_VALUE, rightSection.value)
            assertEquals(OTHER_NET_DELTA, rightSection.delta)
        }
    }

    @Test
    fun `given a view model, when selected date range changes, then has expected orders values`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchOrdersData(any(), any(), eq(Saved)) }.doReturn(getOrdersStats())
            }

            sut = givenAViewModel()
            sut.onSelectedTimePeriodChanged(LAST_YEAR)

            val resourceProvider = givenAResourceProvider()
            with(sut.state.value.ordersState) {
                assertTrue(this is AnalyticsInformationViewState.DataViewState)
                assertEquals(resourceProvider.getString(R.string.analytics_orders_card_title), title)
                assertEquals(resourceProvider.getString(R.string.analytics_total_orders_title), leftSection.title)
                assertEquals(ORDERS_COUNT.toString(), leftSection.value)
                assertEquals(resourceProvider.getString(R.string.analytics_avg_orders_title), rightSection.title)
                assertEquals(ORDERS_COUNT_DELTA, leftSection.delta)
                assertEquals(AVG_CURRENCY_VALUE, rightSection.value)
                assertEquals(AVG_ORDER_VALUE_DELTA, rightSection.delta)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then product has values`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchProductsData(any(), any(), eq(Saved)) }.doReturn(getProductsStats())
            }

            sut = givenAViewModel()
            sut.onSelectedTimePeriodChanged(LAST_YEAR)

            val resourceProvider = givenAResourceProvider()
            with(sut.state.value.productsState) {
                assertTrue(this is AnalyticsListViewState.DataViewState)
                assertEquals(resourceProvider.getString(R.string.analytics_products_card_title), title)
                assertEquals(PRODUCT_ITEMS_SOLD_DELTA, delta)
                assertEquals(resourceProvider.getString(R.string.analytics_products_list_items_sold), subTitle)
                assertEquals(PRODUCT_ITEMS_SOLD.toString(), subTitleValue)
                assertEquals(resourceProvider.getString(R.string.analytics_products_list_header_title), listLeftHeader)
                assertEquals(
                    resourceProvider.getString(R.string.analytics_products_list_header_subtitle),
                    listRightHeader
                )
                assertEquals(PRODUCT_LIST.size, items.size)
            }
        }

    @Test
    fun `given a week to date selected, when refresh is requested, then has expected orders values`() = testBlocking {
        val weekToDateRange = MultipleDateRange(
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
        )

        val weekOrdersData = getOrdersStats(
            OTHER_ORDERS_COUNT,
            OTHER_ORDERS_COUNT_DELTA,
            OTHER_AVG_ORDER_VALUE,
            OTHER_AVG_ORDER_VALUE_DELTA,
            OTHER_CURRENCY_CODE
        )

        whenever(calculator.getAnalyticsDateRangeFrom(WEEK_TO_DATE)) doReturn weekToDateRange
        analyticsRepository.stub {
            onBlocking { fetchOrdersData(weekToDateRange, WEEK_TO_DATE, ForceNew) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onSelectedTimePeriodChanged(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val resourceProvider = givenAResourceProvider()
        with(sut.state.value.ordersState) {
            assertTrue(this is AnalyticsInformationViewState.DataViewState)
            assertEquals(resourceProvider.getString(R.string.analytics_orders_card_title), title)
            assertEquals(resourceProvider.getString(R.string.analytics_total_orders_title), leftSection.title)
            assertEquals(OTHER_ORDERS_COUNT.toString(), leftSection.value)
            assertEquals(OTHER_ORDERS_COUNT_DELTA, leftSection.delta)
            assertEquals(resourceProvider.getString(R.string.analytics_avg_orders_title), rightSection.title)
            assertEquals(OTHER_AVG_CURRENCY_VALUE, rightSection.value)
            assertEquals(OTHER_AVG_ORDER_VALUE_DELTA, rightSection.delta)
        }
    }

    @Test
    fun `given a week to date selected, when refresh is requested, then revenue is the expected`() = testBlocking {
        val weekToDateRange = MultipleDateRange(
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
        )

        val weekRevenueStats = getRevenueStats(
            OTHER_TOTAL_VALUE,
            OTHER_NET_VALUE,
            OTHER_CURRENCY_CODE,
            DeltaPercentage.Value(OTHER_TOTAL_DELTA),
            DeltaPercentage.Value(OTHER_NET_DELTA)
        )

        whenever(calculator.getAnalyticsDateRangeFrom(WEEK_TO_DATE)) doReturn weekToDateRange
        analyticsRepository.stub {
            onBlocking { fetchRevenueData(weekToDateRange, WEEK_TO_DATE, ForceNew) }.doReturn(weekRevenueStats)
        }

        sut = givenAViewModel()
        sut.onSelectedTimePeriodChanged(WEEK_TO_DATE)
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
    fun `given a week to date selected, when refresh is requested, then has expected product values`() = testBlocking {
        val weekToDateRange = MultipleDateRange(
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
        )

        val weekOrdersData = getProductsStats(
            OTHER_PRODUCT_ITEMS_SOLD,
            OTHER_PRODUCT_ITEMS_SOLD_DELTA,
            OTHER_PRODUCT_LIST
        )

        whenever(calculator.getAnalyticsDateRangeFrom(WEEK_TO_DATE)) doReturn weekToDateRange
        analyticsRepository.stub {
            onBlocking { fetchProductsData(weekToDateRange, WEEK_TO_DATE, ForceNew) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onSelectedTimePeriodChanged(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val resourceProvider = givenAResourceProvider()
        with(sut.state.value.productsState) {
            assertTrue(this is AnalyticsListViewState.DataViewState)
            assertEquals(resourceProvider.getString(R.string.analytics_products_card_title), title)
            assertEquals(OTHER_PRODUCT_ITEMS_SOLD_DELTA, delta)
            assertEquals(resourceProvider.getString(R.string.analytics_products_list_items_sold), subTitle)
            assertEquals(OTHER_PRODUCT_ITEMS_SOLD.toString(), subTitleValue)
            assertEquals(resourceProvider.getString(R.string.analytics_products_list_header_title), listLeftHeader)
            assertEquals(
                resourceProvider.getString(R.string.analytics_products_list_header_subtitle),
                listRightHeader
            )
            assertEquals(OTHER_PRODUCT_LIST.size, items.size)
        }
    }

    @Test
    fun `given a view, when refresh is requested, then show indicator is the expected`() = testBlocking {
        analyticsRepository.stub {
            onBlocking { fetchRevenueData(any(), any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any(), any()) }.doReturn(getProductsStats())
        }

        sut = givenAViewModel()
        val states = mutableListOf<AnalyticsViewState>()
        val getShowIndicatorStatesJob = launch { sut.state.toList(states) }
        sut.onRefreshRequested()

        assertThat(states.map(AnalyticsViewState::refreshIndicator)).containsExactly(
            NotShowIndicator,
            RefreshIndicator.ShowIndicator
        )

        getShowIndicatorStatesJob.cancel()
    }

    @Test
    fun `given a view, when custom date range is clicked, then OpenDatePicker event is triggered`() {
        sut = givenAViewModel()
        sut.onCustomDateRangeClicked()

        assertThat(sut.event.value).isInstanceOf(AnalyticsViewEvent.OpenDatePicker::class.java)
    }

    @Test
    fun `when all data is fetched successfully then all transaction conditions are satisfied`() = testBlocking {
        analyticsRepository.stub {
            onBlocking { fetchRevenueData(any(), any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any(), any()) }.doReturn(getProductsStats())
            onBlocking { fetchRecentVisitorsData(any(), any(), any()) }.doReturn(getVisitorStats())
        }

        sut = givenAViewModel()

        verify(transactionLauncher).onRevenueFetched()
        verify(transactionLauncher).onOrdersFetched()
        verify(transactionLauncher).onProductsFetched()
        verify(transactionLauncher).onVisitorsFetched()
    }

    @Test
    fun `when fetch revenue fails then performance transaction revenue condition is not satisfied`() = testBlocking {
        analyticsRepository.stub {
            onBlocking { fetchRevenueData(any(), any(), any()) }.doReturn(RevenueError)
            onBlocking { fetchOrdersData(any(), any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any(), any()) }.doReturn(getProductsStats())
            onBlocking { fetchRecentVisitorsData(any(), any(), any()) }.doReturn(getVisitorStats())
        }

        sut = givenAViewModel()

        verify(transactionLauncher, never()).onRevenueFetched()
        verify(transactionLauncher).onOrdersFetched()
        verify(transactionLauncher).onProductsFetched()
        verify(transactionLauncher).onVisitorsFetched()
    }

    @Test
    fun `when fetch orders fails then performance transaction order condition is not satisfied`() = testBlocking {
        analyticsRepository.stub {
            onBlocking { fetchRevenueData(any(), any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any(), any()) }.doReturn(OrdersError)
            onBlocking { fetchProductsData(any(), any(), any()) }.doReturn(getProductsStats())
            onBlocking { fetchRecentVisitorsData(any(), any(), any()) }.doReturn(getVisitorStats())
        }

        sut = givenAViewModel()

        verify(transactionLauncher).onRevenueFetched()
        verify(transactionLauncher, never()).onOrdersFetched()
        verify(transactionLauncher).onProductsFetched()
        verify(transactionLauncher).onVisitorsFetched()
    }

    @Test
    fun `when fetch products fails then performance transaction products condition is not satisfied`() = testBlocking {
        analyticsRepository.stub {
            onBlocking { fetchRevenueData(any(), any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any(), any()) }.doReturn(ProductsError)
            onBlocking { fetchRecentVisitorsData(any(), any(), any()) }.doReturn(getVisitorStats())
        }

        sut = givenAViewModel()

        verify(transactionLauncher).onRevenueFetched()
        verify(transactionLauncher).onOrdersFetched()
        verify(transactionLauncher, never()).onProductsFetched()
        verify(transactionLauncher).onVisitorsFetched()
    }

    @Test
    fun `when fetch visitors fails then performance transaction visitors condition is not satisfied`() = testBlocking {
        analyticsRepository.stub {
            onBlocking { fetchRevenueData(any(), any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any(), any()) }.doReturn(getProductsStats())
            onBlocking { fetchRecentVisitorsData(any(), any(), any()) }.doReturn(VisitorsError)
        }

        sut = givenAViewModel()

        verify(transactionLauncher).onRevenueFetched()
        verify(transactionLauncher).onOrdersFetched()
        verify(transactionLauncher).onProductsFetched()
        verify(transactionLauncher, never()).onVisitorsFetched()
    }

    @Test
    fun `given a date range selected, then has expected visitors values`() = testBlocking {
        val weekToDateRange = MultipleDateRange(
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
        )

        val weekOrdersData = getVisitorStats()

        whenever(calculator.getAnalyticsDateRangeFrom(WEEK_TO_DATE)) doReturn weekToDateRange
        analyticsRepository.stub {
            onBlocking { fetchRecentVisitorsData(weekToDateRange, WEEK_TO_DATE, Saved) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onSelectedTimePeriodChanged(WEEK_TO_DATE)

        assert(sut.state.value.visitorsState)
    }

    @Test
    fun `given a quarter to date range is selected, then has expected visitors values`() = testBlocking {
        val weekToDateRange = MultipleDateRange(
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
        )

        val weekOrdersData = getVisitorStats()

        whenever(calculator.getAnalyticsDateRangeFrom(QUARTER_TO_DATE)) doReturn weekToDateRange
        analyticsRepository.stub {
            onBlocking { fetchQuarterVisitorsData(weekToDateRange, QUARTER_TO_DATE, Saved) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onSelectedTimePeriodChanged(QUARTER_TO_DATE)

        assert(sut.state.value.visitorsState)
    }

    @Test
    fun `given a last quarter range is selected, then has expected visitors values`() = testBlocking {
        val weekToDateRange = MultipleDateRange(
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
        )

        val weekOrdersData = getVisitorStats()

        whenever(calculator.getAnalyticsDateRangeFrom(LAST_QUARTER)) doReturn weekToDateRange
        analyticsRepository.stub {
            onBlocking { fetchQuarterVisitorsData(weekToDateRange, LAST_QUARTER, Saved) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onSelectedTimePeriodChanged(LAST_QUARTER)

        assert(sut.state.value.visitorsState)
    }

    @Test
    fun `given a custom range is selected, then have no visitors request done`() = testBlocking {
        val weekToDateRange = MultipleDateRange(
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
            SimpleDateRange(ANY_WEEK_DATE, ANY_WEEK_DATE),
        )

        sut = givenAViewModel()
        sut.onSelectedTimePeriodChanged(CUSTOM)

        verify(analyticsRepository, never()).fetchQuarterVisitorsData(weekToDateRange, CUSTOM, Saved)
        verify(analyticsRepository, never()).fetchRecentVisitorsData(weekToDateRange, CUSTOM, Saved)
    }

    private fun givenAResourceProvider(): ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invMock -> invMock.arguments[0].toString() }
        on { getStringArray(any()) } doAnswer { DATE_RANGE_SELECTORS.toTypedArray() }
    }

    private fun givenAViewModel(resourceProvider: ResourceProvider = givenAResourceProvider()): AnalyticsViewModel {
        val analyticsDateRangeFormatter = AnalyticsDateRangeFormatter(dateUtil, resourceProvider)
        return AnalyticsViewModel(
            resourceProvider,
            calculator,
            currencyFormatter,
            analyticsRepository,
            transactionLauncher,
            mock(),
            analyticsDateRangeFormatter,
            savedState
        )
    }

    private fun getRevenueStats(
        totalValue: Double = TOTAL_VALUE,
        netValue: Double = NET_VALUE,
        currencyCode: String = CURRENCY_CODE,
        totalDelta: DeltaPercentage = DeltaPercentage.Value(TOTAL_DELTA.toInt()),
        netDelta: DeltaPercentage = DeltaPercentage.Value(NET_DELTA.toInt()),
    ) = RevenueData(
        RevenueStat(
            totalValue,
            totalDelta,
            netValue,
            netDelta,
            currencyCode,
            listOf(TOTAL_VALUE),
            listOf(NET_VALUE)
        )
    )

    private fun getOrdersStats(
        ordersCount: Int = ORDERS_COUNT,
        ordersCountDelta: Int = ORDERS_COUNT_DELTA,
        avgOrderValue: Double = AVG_ORDER_VALUE,
        avgOrderValueDelta: Int = AVG_ORDER_VALUE_DELTA,
        currencyCode: String = CURRENCY_CODE
    ) = OrdersData(
        OrdersStat(
            ordersCount,
            DeltaPercentage.Value(ordersCountDelta),
            avgOrderValue,
            DeltaPercentage.Value(avgOrderValueDelta),
            currencyCode,
            listOf(ORDERS_COUNT.toLong()),
            listOf(AVG_ORDER_VALUE)
        )
    )

    private fun getProductsStats(
        itemsSold: Int = PRODUCT_ITEMS_SOLD,
        itemsSoldDelta: Int = PRODUCT_ITEMS_SOLD_DELTA,
        productList: List<ProductItem> = PRODUCT_LIST
    ) = ProductsData(ProductsStat(itemsSold, DeltaPercentage.Value(itemsSoldDelta), productList))

    private fun getVisitorStats(
        visitorsCount: Int = DEFAULT_VISITORS_COUNT,
        viewsCount: Int = DEFAULT_VIEWS_COUNT,
        avgVisitorsDelta: DeltaPercentage = DeltaPercentage.Value(DEFAULT_AVG_VISITORS_DELTA),
        avgViewsDelta: DeltaPercentage = DeltaPercentage.Value(DEFAULT_AVG_VIEWS_DELTA)
    ) = VisitorsData(VisitorsStat(visitorsCount, viewsCount, avgVisitorsDelta, avgViewsDelta))

    private fun assert(visitorState: AnalyticsInformationViewState) {
        val resourceProvider = givenAResourceProvider()
        assertThat(visitorState).isEqualTo(
            AnalyticsInformationViewState.DataViewState(
                title = resourceProvider.getString(R.string.analytics_visitors_and_views_card_title),
                leftSection = AnalyticsInformationSectionViewState(
                    title = resourceProvider.getString(R.string.analytics_visitors_subtitle),
                    value = DEFAULT_VISITORS_COUNT.toString(),
                    delta = DEFAULT_AVG_VISITORS_DELTA,
                    chartInfo = emptyList()
                ),
                rightSection = AnalyticsInformationSectionViewState(
                    title = resourceProvider.getString(R.string.analytics_views_subtitle),
                    value = DEFAULT_VIEWS_COUNT.toString(),
                    delta = DEFAULT_AVG_VIEWS_DELTA,
                    chartInfo = emptyList()
                )
            )
        )
    }

    companion object {
        private const val ANY_DATE_TIME_VALUE = "2021-11-21 00:00:00"
        private const val ANY_OTHER_DATE_TIME_VALUE = "2021-11-20 00:00:00"
        private const val ANY_WEEK_DATE_TIME_VALUE = "2010-11-20 00:00:00"

        private const val ANY_YEAR_VALUE = "2021-11-21"
        private const val ANY_SORT_FORMAT_VALUE = "21 Nov, 2021"

        private const val ANY_VALUE = "Today"
        private const val ANY_SAVED_VALUE = "Other year"
        private const val ANY_OTHER_VALUE = "Last year"

        private const val ANY_DATE_RANGE_EXPECTED_DATE_MESSAGE = "$ANY_VALUE ($ANY_SORT_FORMAT_VALUE)"
        private const val ANY_OTHER_RANGE_EXPECTED_DATE_MESSAGE = "$ANY_OTHER_VALUE ($ANY_SORT_FORMAT_VALUE)"
        private const val ANY_SAVED_RANGE_EXPECTED_DATE_MESSAGE = "$ANY_OTHER_VALUE ($ANY_SORT_FORMAT_VALUE)"

        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        private val ANY_DATE: Date = sdf.parse(ANY_DATE_TIME_VALUE)!!
        private val ANY_OTHER_DATE: Date = sdf.parse(ANY_OTHER_DATE_TIME_VALUE)!!
        private val ANY_WEEK_DATE: Date = sdf.parse(ANY_WEEK_DATE_TIME_VALUE)!!
        private val DATE_RANGE_SELECTORS = listOf(ANY_VALUE, ANY_OTHER_VALUE)

        const val TOTAL_VALUE = 10.0
        const val TOTAL_DELTA = 5.0
        const val NET_VALUE = 20.0
        const val NET_DELTA = 10.0
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

        const val PRODUCT_ITEMS_SOLD = 1
        const val PRODUCT_ITEMS_SOLD_DELTA = 50
        const val PRODUCT_CURRENCY_VALUE = "50 E"
        const val OTHER_PRODUCT_ITEMS_SOLD = 3
        const val OTHER_PRODUCT_ITEMS_SOLD_DELTA = 10
        const val OTHER_PRODUCT_CURRENCY_VALUE = "55 E"

        private const val PRODUCT_ONE_QUANTITY = 1
        private const val PRODUCT_MORE_THAN_ONE_QUANTITY = 10
        private const val PRODUCT_NET_SALES = 1.toDouble()
        private const val OTHER_PRODUCT_NET_SALES = 2.toDouble()
        private const val PRODUCT_ITEM_IMAGE = "image"
        private const val PRODUCT_ITEM_NAME = "product"
        private const val PRODUCT_CURRENCY_CODE = "EUR"

        val PRODUCT_LIST = listOf(
            ProductItem(
                PRODUCT_ITEM_NAME,
                PRODUCT_NET_SALES,
                PRODUCT_ITEM_IMAGE,
                PRODUCT_ONE_QUANTITY,
                PRODUCT_CURRENCY_CODE
            ),
            ProductItem(
                PRODUCT_ITEM_NAME,
                PRODUCT_NET_SALES,
                PRODUCT_ITEM_IMAGE,
                PRODUCT_MORE_THAN_ONE_QUANTITY,
                PRODUCT_CURRENCY_CODE
            )
        ).sortedByDescending { it.quantity }

        val OTHER_PRODUCT_LIST = listOf(
            ProductItem(
                PRODUCT_ITEM_NAME,
                PRODUCT_NET_SALES,
                PRODUCT_ITEM_IMAGE,
                PRODUCT_ONE_QUANTITY,
                PRODUCT_CURRENCY_CODE
            )
        ).sortedByDescending { it.quantity }

        private const val DEFAULT_VISITORS_COUNT = 100
        private const val DEFAULT_VIEWS_COUNT = 100
        private const val DEFAULT_AVG_VISITORS_DELTA = 10
        private const val DEFAULT_AVG_VIEWS_DELTA = 34

        const val ANY_URL = "https://a8c.com"
        const val ORDERS_COUNT = 5
        const val OTHER_ORDERS_COUNT = 50
        const val ORDERS_COUNT_DELTA = 20
        const val OTHER_ORDERS_COUNT_DELTA = 1
        const val AVG_ORDER_VALUE = 11.2
        const val OTHER_AVG_ORDER_VALUE = 44.21
        const val AVG_ORDER_VALUE_DELTA = 50
        const val OTHER_AVG_ORDER_VALUE_DELTA = 1
        const val AVG_CURRENCY_VALUE = "11.20 E"
        const val OTHER_AVG_CURRENCY_VALUE = "44.21 E"
    }
}
