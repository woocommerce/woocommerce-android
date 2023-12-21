package com.woocommerce.android.ui.analytics

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.ProductItem
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.Companion.ANALYTICS_ORDERS_PATH
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.Companion.ANALYTICS_PRODUCTS_PATH
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.Companion.ANALYTICS_REVENUE_PATH
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.FetchStrategy.ForceNew
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.FetchStrategy.Saved
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.OrdersResult.OrdersData
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.OrdersResult.OrdersError
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.ProductsResult.ProductsData
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.ProductsResult.ProductsError
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
class AnalyticsRepositoryTest : BaseUnitTest() {
    private val statsRepository: StatsRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()

    private lateinit var testTimeZone: TimeZone
    private lateinit var testLocale: Locale
    private lateinit var testCalendar: Calendar

    private lateinit var testSelectionData: StatsTimeRangeSelection
    private lateinit var currentStart: String
    private lateinit var currentEnd: String
    private lateinit var previousStart: String
    private lateinit var previousEnd: String

    private val sut: AnalyticsRepository = AnalyticsRepository(
        statsRepository,
        selectedSite,
        wooCommerceStore,
        coroutinesTestRule.testDispatchers
    )

    @Before
    fun setUp() {
        testLocale = Locale.UK
        testTimeZone = TimeZone.getDefault()
        testCalendar = Calendar.getInstance(testLocale)
        testCalendar.timeZone = testTimeZone
        testCalendar.firstDayOfWeek = Calendar.MONDAY

        testSelectionData = generateTestSelectionData(
            referenceStartDate = "2022-09-25".dayStartFrom(),
            referenceEndDate = "2022-04-10".dayEndFrom()
        )

        currentStart = testSelectionData.currentRange.start.formatToYYYYmmDDhhmmss()
        currentEnd = testSelectionData.currentRange.end.formatToYYYYmmDDhhmmss()
        previousStart = testSelectionData.previousRange.start.formatToYYYYmmDDhhmmss()
        previousEnd = testSelectionData.previousRange.end.formatToYYYYmmDDhhmmss()
    }

    @Test
    fun `given no currentPeriodRevenue, when fetchRevenueData, then result is RevenueError`() = runTest {
        // Given
        val previousPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
            .thenReturn(listOf(Result.success(previousPeriodRevenue)).asFlow())

        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
            .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

        // When
        val result = sut.fetchRevenueData(
            testSelectionData,
            anyFetchStrategy
        )

        // Then
        assertTrue(result is RevenueError)
    }

    @Test
    fun `given no currentPeriodRevenue when fetchOrderData result is RevenueError`() = runTest {
        // Given
        val previousPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
            .thenReturn(listOf(Result.success(previousPeriodOrdersStats)).asFlow())

        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
            .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

        // When
        val result = sut.fetchOrdersData(
            testSelectionData,
            anyFetchStrategy
        )

        // Then
        assertTrue(result is OrdersError)
    }

    @Test
    fun `given no previousRevenuePeriod, when fetchRevenueData, then result is RevenueError`() = runTest {
        // Given
        val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
            .thenReturn(listOf(Result.success(currentPeriodRevenue)).asFlow())

        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
            .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

        // When
        val result = sut.fetchRevenueData(
            testSelectionData,
            anyFetchStrategy
        )

        // Then
        assertTrue(result is RevenueError)
    }

    @Test
    fun `given no previousRevenuePeriod when fetchOrdersData result is OrdersError`() =
        runTest {
            // Given
            val currentPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success(currentPeriodOrdersStats)).asFlow())

            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

            // When
            val result = sut.fetchOrdersData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is OrdersError)
        }

    @Test
    fun `given previous and current period revenue, when fetchRevenueData, then result is the expected`() =
        runTest {
            // Given
            val previousRevenue = givenARevenue(
                totalSales = 100.0,
                netValue = 80.0,
                itemsSold = 10
            )
            val currentRevenue = givenARevenue(
                totalSales = 120.0,
                netValue = 100.0,
                itemsSold = 12
            )
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success(currentRevenue)).asFlow())

            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success(previousRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertThat(result).isEqualTo(
                RevenueData(
                    RevenueStat(
                        totalValue = 120.0,
                        netValue = 100.0,
                        totalDelta = DeltaPercentage.Value(20),
                        netDelta = DeltaPercentage.Value(25),
                        currencyCode = null,
                        totalRevenueByInterval = listOf(120.0),
                        netRevenueByInterval = listOf(100.0)
                    )
                )
            )
        }

    @Test
    fun `given previous and current period revenue, when fetchRevenueData, then delta values are rounded`() =
        runTest {
            // Given
            val previousRevenue = givenARevenue(
                totalSales = 100.0,
                netValue = 80.0,
                itemsSold = 10
            )
            val currentRevenue = givenARevenue(
                totalSales = 199.5,
                netValue = 159.4,
                itemsSold = 12
            )
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success(currentRevenue)).asFlow())

            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success(previousRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertThat(result).isEqualTo(
                RevenueData(
                    RevenueStat(
                        totalValue = 199.5,
                        netValue = 159.4,
                        totalDelta = DeltaPercentage.Value(100),
                        netDelta = DeltaPercentage.Value(99),
                        currencyCode = null,
                        totalRevenueByInterval = listOf(199.5),
                        netRevenueByInterval = listOf(159.4)
                    )
                )
            )
        }

    @Test
    fun `given previous revenue and current zero revenue, when fetchRevenueData, then deltas are the expected`() =
        runTest {
            // Given
            val revenue = givenARevenue(ZERO_VALUE, ZERO_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            val previousRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success(previousRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is RevenueData)
            assertTrue(result.revenueStat.totalDelta is DeltaPercentage.Value)
            assertEquals(ONE_HUNDRED_DECREASE, (result.revenueStat.totalDelta as DeltaPercentage.Value).value)
            assertTrue(result.revenueStat.netDelta is DeltaPercentage.Value)
            assertEquals(ONE_HUNDRED_DECREASE, (result.revenueStat.netDelta as DeltaPercentage.Value).value)
        }

    @Test
    fun `given zero previous and current revenue, when fetchRevenueData, then deltas are the expected`() =
        runTest {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            val previousRevenue = givenARevenue(ZERO_VALUE, ZERO_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success(previousRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                testSelectionData,
                anyFetchStrategy
            )
            // Then
            assertTrue(result is RevenueData)
            assertTrue(result.revenueStat.totalDelta is DeltaPercentage.NotExist)
            assertTrue(result.revenueStat.netDelta is DeltaPercentage.NotExist)
        }

    @Test
    fun `given zero previous and current revenue, when fetchOrdersData, then deltas are the expected`() =
        runTest {
            // Given
            val ordersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success(ordersStats)).asFlow())

            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success(ordersStats)).asFlow())

            // When
            val result = sut.fetchOrdersData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is OrdersData)
            assertTrue(result.ordersStat.avgOrderDelta is DeltaPercentage.Value)
            assertTrue(result.ordersStat.ordersCountDelta is DeltaPercentage.Value)
            assertEquals(ZERO_DELTA, (result.ordersStat.avgOrderDelta as DeltaPercentage.Value).value)
            assertEquals(ZERO_DELTA, (result.ordersStat.ordersCountDelta as DeltaPercentage.Value).value)
        }

    @Test
    fun `given previous and current period revenue when fetchOrdersData result is the expected`() =
        runTest {
            // Given
            val ordersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success(ordersStats)).asFlow())

            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success(ordersStats)).asFlow())

            // When
            val result = sut.fetchOrdersData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is OrdersData)
            assertEquals(TEN_VALUE.toInt(), result.ordersStat.ordersCount)
            assertEquals(TEN_VALUE, result.ordersStat.avgOrderValue)
        }

    @Test
    fun `given zero previous total revenue, when fetchRevenueData, then result is the expected`() =
        runTest {
            // Given
            val previousPeriodRevenue = givenARevenue(ZERO_VALUE, ZERO_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertNotNull(this)
            assertTrue(result is RevenueData)
            assertEquals(TEN_VALUE, result.revenueStat.totalValue)
            assertEquals(TEN_VALUE, result.revenueStat.netValue)
        }

    @Test
    fun `given zero previous orders revenue when fetchOrdersData result is the expected`() =
        runTest {
            // Given
            val previousPeriodOrdersStats = givenRevenueOrderStats(ZERO_VALUE.toInt(), ZERO_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats)).asFlow())

            val currentPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodOrdersStats)).asFlow())

            // When
            val result = sut.fetchOrdersData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is OrdersData)
            assertEquals(TEN_VALUE.toInt(), result.ordersStat.ordersCount)
            assertEquals(TEN_VALUE, result.ordersStat.avgOrderValue)
            assertTrue(result.ordersStat.ordersCountDelta is DeltaPercentage.NotExist)
            assertTrue(result.ordersStat.avgOrderDelta is DeltaPercentage.NotExist)
        }

    @Test
    fun `given zero previous net revenue, when fetchRevenueData, then result is the expected`() =
        runTest {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, ZERO_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is RevenueData)
            assertEquals(TEN_VALUE, result.revenueStat.totalValue)
            assertEquals(TEN_VALUE, result.revenueStat.netValue)
        }

    @Test
    fun `given zero previous avg order, when fetchOrderData, result is the expected`() =
        runTest {
            // Given
            val previousPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), ZERO_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats)).asFlow())

            val currentPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodOrdersStats)).asFlow())

            // When
            val result = sut.fetchOrdersData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is OrdersData)
            assertEquals(TEN_VALUE.toInt(), result.ordersStat.ordersCount)
            assertEquals(TEN_VALUE, result.ordersStat.avgOrderValue)
        }

    @Test
    fun `given null previous total revenue, when fetchRevenueData, then result is the expected`() =
        runTest {
            // Given
            val previousPeriodRevenue = givenARevenue(null, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is RevenueData)
            assertEquals(TEN_VALUE, result.revenueStat.totalValue)
        }

    @Test
    fun `given null previous orders, when fetchOrdersData, then result is the expected`() =
        runTest {
            // Given
            val previousPeriodOrdersStats = givenRevenueOrderStats(null, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats)).asFlow())

            val currentPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodOrdersStats)).asFlow())

            // When
            val result = sut.fetchOrdersData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is OrdersData)
            assertEquals(TEN_VALUE.toInt(), result.ordersStat.ordersCount)
            assertEquals(TEN_VALUE, result.ordersStat.avgOrderValue)
        }

    @Test
    fun `given null previous net revenue, when fetchRevenueData, then result is the expected`() =
        runTest {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, null, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is RevenueData)
            assertEquals(TEN_VALUE, result.revenueStat.totalValue)
            assertEquals(TEN_VALUE, result.revenueStat.netValue)
        }

    @Test
    fun `given null previous avg order, when fetchOrdersData, then result is the expected`() =
        runTest {
            // Given
            val previousPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), null)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats)).asFlow())

            val currentPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodOrdersStats)).asFlow())

            // When
            val result = sut.fetchOrdersData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is OrdersData)
            assertEquals(TEN_VALUE.toInt(), result.ordersStat.ordersCount)
        }

    @Test
    fun `given previous and current revenue, when fetchRevenueData multiple date range, then result is the expected`() =
        runTest {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is RevenueData)
            assertEquals(TEN_VALUE, result.revenueStat.totalValue)
            assertEquals(TEN_VALUE, result.revenueStat.netValue)
        }

    @Test
    fun `given previous and current period revenue, when fetchOrdersData multiple date range, result is expected`() =
        runTest {
            // Given
            val previousPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats)).asFlow())

            val currentPeriodRevenue = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchOrdersData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is OrdersData)
            assertEquals(TEN_VALUE.toInt(), result.ordersStat.ordersCount)
            assertEquals(TEN_VALUE, result.ordersStat.avgOrderValue)
        }

    @Test
    fun `given no currentPeriodRevenue, when fetchProductsData, then result is ProductsError`() =
        runTest {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success(previousPeriodRevenue)).asFlow())

            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

            whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
                .thenReturn(Result.success(Unit))

            val productLeaderBoards = givenAProductsStats()
            whenever(statsRepository.getTopPerformers(any(), any()))
                .thenReturn(productLeaderBoards)

            // When
            val result = sut.fetchProductsData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is ProductsError)
        }

    @Test
    fun `given no previousPeriodRevenue, when fetchProductsData, then result is ProductsError`() =
        runTest {
            // Given
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success(currentPeriodRevenue)).asFlow())

            whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
                .thenReturn(Result.success(Unit))

            val productLeaderBoards = givenAProductsStats()
            whenever(statsRepository.getTopPerformers(any(), any()))
                .thenReturn(productLeaderBoards)

            // When
            val result = sut.fetchProductsData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is ProductsError)
        }

    @Test
    fun `given previousPeriodRevenue with null items sold, when fetchProductsData, then result is ProductsError`() =
        runTest {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success(previousPeriodRevenue)).asFlow())

            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

            whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
                .thenReturn(Result.success(Unit))

            val productLeaderBoards = givenAProductsStats()
            whenever(statsRepository.getTopPerformers(any(), any()))
                .thenReturn(productLeaderBoards)

            // When
            val result = sut.fetchProductsData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is ProductsError)
        }

    @Test
    fun `given currentPeriodRevenue with null items sold, when fetchProductsData, then result is ProductsError`() =
        runTest {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(previousStart), eq(previousEnd), any()))
                .thenReturn(listOf(Result.success(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, null)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(currentStart), eq(currentEnd), any()))
                .thenReturn(listOf(Result.success(currentPeriodRevenue)).asFlow())

            whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
                .thenReturn(Result.success(Unit))

            val productLeaderBoards = givenAProductsStats()
            whenever(statsRepository.getTopPerformers(any(), any()))
                .thenReturn(productLeaderBoards)

            // When
            val result = sut.fetchProductsData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is ProductsError)
        }

    @Test
    fun `given no products leader board with null items sold, when fetchProductsData, then result is ProductsError`() =
        runTest {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any(), any()))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
                .thenReturn(Result.failure(NullPointerException()))

            // When
            val result = sut.fetchProductsData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is ProductsError)
        }

    @Test
    fun `given products and revenue with null items sold, when fetchProductsData, then result is expected`() =
        runTest {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any(), any()))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
                .thenReturn(Result.success(Unit))

            val productLeaderBoards = givenAProductsStats()
            whenever(statsRepository.getTopPerformers(any(), any()))
                .thenReturn(productLeaderBoards)

            // When
            val result = sut.fetchProductsData(
                testSelectionData,
                anyFetchStrategy
            )

            // Then
            assertTrue(result is ProductsData)
            assertEquals(TEN_VALUE.toInt(), result.productsStat.itemsSold)
            assertEquals(DeltaPercentage.Value(ZERO_DELTA), result.productsStat.itemsSoldDelta)
            assertEquals(expectedProducts, result.productsStat.products)
        }

    @Test
    fun `when get revenue and products data at same time with ForceNew fetch strategy, then stats repository is used once per period`() =
        runTest {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any(), any()))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
                .thenReturn(Result.success(Unit))

            val productLeaderBoards = givenAProductsStats()
            whenever(statsRepository.getTopPerformers(any(), any()))
                .thenReturn(productLeaderBoards)

            val siteModel: SiteModel = mock()
            whenever(selectedSite.get()).thenReturn(siteModel)

            // When
            sut.fetchRevenueData(testSelectionData, ForceNew)

            sut.fetchProductsData(testSelectionData, ForceNew)

            // Then
            verify(statsRepository, times(2)).fetchRevenueStats(
                any(), any(), eq(previousStart), eq(previousEnd), any()
            )
        }

    @Test
    fun `when get revenue and products data at same time with Saved fetch strategy, then stats repository is used once per period`() =
        runTest {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.getRevenueStatsById(any()))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
                .thenReturn(Result.success(Unit))

            val productLeaderBoards = givenAProductsStats()
            whenever(statsRepository.getTopPerformers(any(), any()))
                .thenReturn(productLeaderBoards)

            val siteModel: SiteModel = mock()
            whenever(selectedSite.get()).thenReturn(siteModel)

            // When
            sut.fetchRevenueData(testSelectionData, Saved)

            sut.fetchProductsData(testSelectionData, Saved)

            // Then
            verify(statsRepository, times(2)).getRevenueStatsById(any())
        }

    @Test
    fun `when force get new revenue and products data at same time, then stats repository is used twice`() =
        runTest {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any(), any()))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
                .thenReturn(Result.success(Unit))

            val productLeaderBoards = givenAProductsStats()
            whenever(statsRepository.getTopPerformers(any(), any()))
                .thenReturn(productLeaderBoards)

            val siteModel: SiteModel = mock()
            whenever(selectedSite.get()).thenReturn(siteModel)

            // When
            sut.fetchRevenueData(
                testSelectionData,
                ForceNew
            )

            sut.fetchProductsData(
                testSelectionData,
                ForceNew
            )

            // Then
            verify(statsRepository, times(2)).fetchRevenueStats(
                any(),
                any(),
                eq(previousStart),
                eq(previousEnd),
                any()
            )
            verify(statsRepository, times(2)).fetchRevenueStats(
                any(),
                any(),
                eq(currentStart),
                eq(currentEnd),
                any()
            )
        }

    @Test
    fun `when get revenue admin url panel, then is expected`() {
        val siteModel: SiteModel = mock()
        whenever(siteModel.adminUrl).thenReturn(ANY_URL)
        whenever(selectedSite.getIfExists()).thenReturn(siteModel)

        val adminPanelUrl = sut.getRevenueAdminPanelUrl()

        assertEquals(ANY_URL + ANALYTICS_REVENUE_PATH, adminPanelUrl)
    }

    @Test
    fun `when get orders admin url panel, then is expected`() {
        val siteModel: SiteModel = mock()
        whenever(siteModel.adminUrl).thenReturn(ANY_URL)
        whenever(selectedSite.getIfExists()).thenReturn(siteModel)

        val adminPanelUrl = sut.getOrdersAdminPanelUrl()

        assertEquals(ANY_URL + ANALYTICS_ORDERS_PATH, adminPanelUrl)
    }

    @Test
    fun `when get products admin url panel, then is expected`() {
        val siteModel: SiteModel = mock()
        whenever(siteModel.adminUrl).thenReturn(ANY_URL)
        whenever(selectedSite.getIfExists()).thenReturn(siteModel)

        val adminPanelUrl = sut.getProductsAdminPanelUrl()

        assertEquals(ANY_URL + ANALYTICS_PRODUCTS_PATH, adminPanelUrl)
    }

    private fun givenARevenue(totalSales: Double?, netValue: Double?, itemsSold: Int?): WCRevenueStatsModel {
        val stats: WCRevenueStatsModel = mock()
        val interval: WCRevenueStatsModel.Interval = mock()
        val subtotal: WCRevenueStatsModel.SubTotal = mock()
        val revenueStatsTotal: WCRevenueStatsModel.Total = mock()
        whenever(revenueStatsTotal.totalSales).thenReturn(totalSales)
        whenever(revenueStatsTotal.netRevenue).thenReturn(netValue)
        whenever(revenueStatsTotal.itemsSold).thenReturn(itemsSold)
        whenever(stats.parseTotal()).thenReturn(revenueStatsTotal)
        whenever(subtotal.totalSales).thenReturn(totalSales)
        whenever(subtotal.netRevenue).thenReturn(netValue)
        whenever(interval.subtotals).thenReturn(subtotal)
        whenever(stats.getIntervalList()).thenReturn(listOf(interval))
        return stats
    }

    private fun givenRevenueOrderStats(orders: Int?, avgOrderValue: Double?): WCRevenueStatsModel {
        val stats: WCRevenueStatsModel = mock()
        val revenueStatsTotal: WCRevenueStatsModel.Total = mock()
        whenever(revenueStatsTotal.ordersCount).thenReturn(orders)
        whenever(revenueStatsTotal.avgOrderValue).thenReturn(avgOrderValue)
        whenever(stats.parseTotal()).thenReturn(revenueStatsTotal)
        return stats
    }

    private fun givenAProductsStats(): List<TopPerformerProductEntity> {
        val product: WCProductModel = mock()
        whenever(product.name).thenReturn(NAME)
        whenever(product.getFirstImageUrl()).thenReturn(IMAGE_URL)
        whenever(product.remoteId).thenReturn(RemoteId(0))

        val productEntity = TopPerformerProductEntity(
            localSiteId = LocalId(0),
            datePeriod = "2021-01-01-2021-01-02",
            productId = product.remoteId,
            name = product.name,
            imageUrl = product.getFirstImageUrl(),
            quantity = TEN_VALUE.toInt(),
            currency = CURRENCY,
            total = TEN_VALUE,
            millisSinceLastUpdated = 0
        )
        return mutableListOf(productEntity)
    }

    private fun String.dayEndFrom(): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = testTimeZone
        val referenceDate = formatter.parse(this)!!
        testCalendar.time = referenceDate
        return testCalendar.endOfCurrentDay()
    }

    private fun String.dayStartFrom(): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = testTimeZone
        val referenceDate = formatter.parse(this)!!
        testCalendar.time = referenceDate
        return testCalendar.startOfCurrentDay()
    }

    private fun generateTestSelectionData(
        referenceStartDate: Date,
        referenceEndDate: Date
    ) = CUSTOM.generateSelectionData(
        referenceStartDate = referenceStartDate,
        referenceEndDate = referenceEndDate,
        calendar = testCalendar,
        locale = testLocale,
        dateUtils = mock()
    )

    companion object {
        const val TEN_VALUE = 10.0
        const val ZERO_VALUE = 0.0

        const val ZERO_DELTA = 0
        const val ONE_HUNDRED_DECREASE = -100

        const val ANY_URL = "https://a8c.com"
        val anyFetchStrategy = ForceNew

        const val CURRENCY = "EUR"
        const val IMAGE_URL = "url"
        const val NAME = "name"

        val expectedProducts = listOf(
            ProductItem(
                NAME,
                TEN_VALUE,
                IMAGE_URL,
                TEN_VALUE.toInt(),
                CURRENCY
            )
        )
    }
}
