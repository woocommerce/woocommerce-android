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
import com.woocommerce.android.ui.analytics.RefreshIndicator.NotShowIndicator
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.LoadingViewState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_QUARTER
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_YEAR
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.QUARTER_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
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
                on { getString(any()) } doReturn TODAY.description
            }

            sut = givenAViewModel(resourceProvider)

            val expectedSelection = TODAY.generateSelectionData()

            with(sut.viewState.value.analyticsDateRangeSelectorState) {
                assertEquals(expectedSelection.selectionType.description, selectedPeriod)
                assertEquals(expectedSelection.currentRangeDescription, toDatePeriod)
                assertEquals(expectedSelection.previousRangeDescription, fromDatePeriod)
            }

            with(sut.viewState.value.revenueState) {
                assertTrue(this is LoadingViewState)
            }

            with(sut.viewState.value.ordersState) {
                assertTrue(this is LoadingViewState)
            }

            with(sut.viewState.value.productsState) {
                assertTrue(this is AnalyticsListViewState.LoadingViewState)
            }

            with(sut.viewState.value.refreshIndicator) {
                assertTrue(this is NotShowIndicator)
            }
        }

    @Test
    fun `when ViewModel is with savedState is created, then has the expected values`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchRevenueData(any(), eq(Saved)) }.doReturn(getRevenueStats())
                onBlocking { fetchOrdersData(any(), eq(Saved)) }.doReturn(getOrdersStats())
            }

            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturn TODAY.description
            }

            sut = givenAViewModel(resourceProvider)

            val expectedSelection = TODAY.generateSelectionData()

            with(sut.viewState.value.analyticsDateRangeSelectorState) {
                assertEquals(expectedSelection.selectionType.description, selectedPeriod)
                assertEquals(expectedSelection.currentRangeDescription, toDatePeriod)
                assertEquals(expectedSelection.previousRangeDescription, fromDatePeriod)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has the expected date range selector values`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchRevenueData(any(), eq(Saved)) }.doReturn(getRevenueStats())
                onBlocking { fetchOrdersData(any(), eq(Saved)) }.doReturn(getOrdersStats())
            }

            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturnConsecutively
                    listOf(ANY_VALUE, LAST_YEAR.description)
            }

            sut = givenAViewModel(resourceProvider)
            sut.onNewRangeSelection(LAST_YEAR)

            val expectedSelection = LAST_YEAR.generateSelectionData()

            with(sut.viewState.value.analyticsDateRangeSelectorState) {
                assertEquals(expectedSelection.selectionType.description, selectedPeriod)
                assertEquals(expectedSelection.currentRangeDescription, toDatePeriod)
                assertEquals(expectedSelection.previousRangeDescription, fromDatePeriod)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has expected revenue values`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchRevenueData(any(), eq(Saved)) }.doReturn(getRevenueStats())
            }

            sut = givenAViewModel()
            sut.onNewRangeSelection(LAST_YEAR)

            val resourceProvider = givenAResourceProvider()
            with(sut.viewState.value.revenueState) {
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
            whenever(analyticsRepository.fetchRevenueData(any(), eq(Saved)))
                .thenReturn(
                    getRevenueStats(
                        netDelta = DeltaPercentage.NotExist,
                        totalDelta = DeltaPercentage.NotExist
                    )
                )

            sut = givenAViewModel()
            sut.onNewRangeSelection(LAST_YEAR)

            with(sut.viewState.value.revenueState) {
                assertTrue(this is AnalyticsInformationViewState.DataViewState)
                assertTrue(leftSection.delta == null)
                assertTrue(rightSection.delta == null)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has expected refresh indicator value`() =
        testBlocking {
            analyticsRepository.stub {
                onBlocking { fetchRevenueData(any(), eq(Saved)) }.doReturn(getRevenueStats())
            }

            sut = givenAViewModel()

            sut.onNewRangeSelection(LAST_YEAR)

            with(sut.viewState.value.refreshIndicator) {
                assertTrue(this is NotShowIndicator)
            }
        }

    @Test
    fun `given a week to date selected, when refresh is requested, then has expected revenue values`() = testBlocking {
        val weekRevenueStats = getRevenueStats(
            OTHER_TOTAL_VALUE,
            OTHER_NET_VALUE,
            OTHER_CURRENCY_CODE,
            DeltaPercentage.Value(OTHER_TOTAL_DELTA),
            DeltaPercentage.Value(OTHER_NET_DELTA),
        )

        analyticsRepository.stub {
            onBlocking { fetchRevenueData(any(), eq(ForceNew)) }.doReturn(weekRevenueStats)
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val resourceProvider = givenAResourceProvider()
        with(sut.viewState.value.revenueState) {
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
                onBlocking { fetchOrdersData(any(), eq(Saved)) }.doReturn(getOrdersStats())
            }

            sut = givenAViewModel()
            sut.onNewRangeSelection(LAST_YEAR)

            val resourceProvider = givenAResourceProvider()
            with(sut.viewState.value.ordersState) {
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
                onBlocking { fetchProductsData(any(), eq(Saved)) }.doReturn(getProductsStats())
            }

            sut = givenAViewModel()
            sut.onNewRangeSelection(LAST_YEAR)

            val resourceProvider = givenAResourceProvider()
            with(sut.viewState.value.productsState) {
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
        val weekOrdersData = getOrdersStats(
            OTHER_ORDERS_COUNT,
            OTHER_ORDERS_COUNT_DELTA,
            OTHER_AVG_ORDER_VALUE,
            OTHER_AVG_ORDER_VALUE_DELTA,
            OTHER_CURRENCY_CODE
        )

        analyticsRepository.stub {
            onBlocking { fetchOrdersData(any(), eq(ForceNew)) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val resourceProvider = givenAResourceProvider()
        with(sut.viewState.value.ordersState) {
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
        val weekRevenueStats = getRevenueStats(
            OTHER_TOTAL_VALUE,
            OTHER_NET_VALUE,
            OTHER_CURRENCY_CODE,
            DeltaPercentage.Value(OTHER_TOTAL_DELTA),
            DeltaPercentage.Value(OTHER_NET_DELTA)
        )

        analyticsRepository.stub {
            onBlocking { fetchRevenueData(any(), eq(ForceNew)) }.doReturn(weekRevenueStats)
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)
        sut.onRefreshRequested()

        with(sut.viewState.value.revenueState) {
            assertTrue(this is AnalyticsInformationViewState.DataViewState)
            assertEquals(OTHER_TOTAL_CURRENCY_VALUE, leftSection.value)
            assertEquals(OTHER_TOTAL_DELTA, leftSection.delta)
            assertEquals(OTHER_NET_CURRENCY_VALUE, rightSection.value)
            assertEquals(OTHER_NET_DELTA, rightSection.delta)
        }
    }

    @Test
    fun `given a week to date selected, when refresh is requested, then has expected product values`() = testBlocking {
        val weekOrdersData = getProductsStats(
            OTHER_PRODUCT_ITEMS_SOLD,
            OTHER_PRODUCT_ITEMS_SOLD_DELTA,
            OTHER_PRODUCT_LIST
        )

        analyticsRepository.stub {
            onBlocking { fetchProductsData(any(), eq(ForceNew)) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val resourceProvider = givenAResourceProvider()
        with(sut.viewState.value.productsState) {
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
            onBlocking { fetchRevenueData(any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any()) }.doReturn(getProductsStats())
        }

        sut = givenAViewModel()
        val states = mutableListOf<AnalyticsViewState>()
        val getShowIndicatorStatesJob = launch { sut.viewState.toList(states) }
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
            onBlocking { fetchRevenueData(any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any()) }.doReturn(getProductsStats())
            onBlocking { fetchRecentVisitorsData(any(), any()) }.doReturn(getVisitorStats())
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
            onBlocking { fetchRevenueData(any(), any()) }.doReturn(RevenueError)
            onBlocking { fetchOrdersData(any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any()) }.doReturn(getProductsStats())
            onBlocking { fetchRecentVisitorsData(any(), any()) }.doReturn(getVisitorStats())
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
            onBlocking { fetchRevenueData(any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any()) }.doReturn(OrdersError)
            onBlocking { fetchProductsData(any(), any()) }.doReturn(getProductsStats())
            onBlocking { fetchRecentVisitorsData(any(), any()) }.doReturn(getVisitorStats())
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
            onBlocking { fetchRevenueData(any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any()) }.doReturn(ProductsError)
            onBlocking { fetchRecentVisitorsData(any(), any()) }.doReturn(getVisitorStats())
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
            onBlocking { fetchRevenueData(any(), any()) }.doReturn(getRevenueStats())
            onBlocking { fetchOrdersData(any(), any()) }.doReturn(getOrdersStats())
            onBlocking { fetchProductsData(any(), any()) }.doReturn(getProductsStats())
            onBlocking { fetchRecentVisitorsData(any(), any()) }.doReturn(VisitorsError)
        }

        sut = givenAViewModel()

        verify(transactionLauncher).onRevenueFetched()
        verify(transactionLauncher).onOrdersFetched()
        verify(transactionLauncher).onProductsFetched()
        verify(transactionLauncher, never()).onVisitorsFetched()
    }

    @Test
    fun `given a date range selected, then has expected visitors values`() = testBlocking {
        val weekOrdersData = getVisitorStats()

        analyticsRepository.stub {
            onBlocking { fetchRecentVisitorsData(any(), eq(Saved)) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)

        assert(sut.viewState.value.visitorsState)
    }

    @Test
    fun `given a quarter to date range is selected, then has expected visitors values`() = testBlocking {
        val weekOrdersData = getVisitorStats()

        analyticsRepository.stub {
            onBlocking { fetchQuarterVisitorsData(any(), eq(Saved)) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(QUARTER_TO_DATE)

        assert(sut.viewState.value.visitorsState)
    }

    @Test
    fun `given a last quarter range is selected, then has expected visitors values`() = testBlocking {
        val weekOrdersData = getVisitorStats()

        analyticsRepository.stub {
            onBlocking { fetchQuarterVisitorsData(any(), eq(Saved)) }.doReturn(weekOrdersData)
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(LAST_QUARTER)

        assert(sut.viewState.value.visitorsState)
    }

    @Test
    fun `given a custom range is selected, then have no visitors request done`() = testBlocking {
        sut = givenAViewModel()
        sut.onNewRangeSelection(CUSTOM)

        verify(analyticsRepository, never()).fetchQuarterVisitorsData(any(), eq(Saved))
        verify(analyticsRepository, never()).fetchRecentVisitorsData(any(), eq(Saved))
    }

    private fun givenAResourceProvider(): ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invMock -> invMock.arguments[0].toString() }
        on { getStringArray(any()) } doAnswer { DATE_RANGE_SELECTORS.toTypedArray() }
    }

    private fun givenAViewModel(resourceProvider: ResourceProvider = givenAResourceProvider()): AnalyticsViewModel {
        return AnalyticsViewModel(
            resourceProvider,
            currencyFormatter,
            analyticsRepository,
            transactionLauncher,
            mock(),
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
        private const val ANY_OTHER_VALUE = "Last year"

        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
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
