package com.woocommerce.android.ui.analytics

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.ProductItem
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository
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
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.wordpress.android.fluxc.model.WCBundleStats
import org.wordpress.android.fluxc.model.WCGiftCardStats
import org.wordpress.android.fluxc.model.WCProductBundleItemReport
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCGoogleStore
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
    private val googleStore: WCGoogleStore = mock()

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
        googleStore,
        wooCommerceStore
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
        whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
            .thenReturn(Result.success(previousPeriodRevenue))

        whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
            .thenReturn(Result.failure(StatsRepository.StatsException(null)))

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
        whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
            .thenReturn(Result.success(previousPeriodOrdersStats))

        whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
            .thenReturn(Result.failure(StatsRepository.StatsException(null)))

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
        whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
            .thenReturn(Result.success(currentPeriodRevenue))

        whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
            .thenReturn(Result.failure(StatsRepository.StatsException(null)))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success(currentPeriodOrdersStats))

            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.failure(StatsRepository.StatsException(null)))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success(currentRevenue))

            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success(previousRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success(currentRevenue))

            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success(previousRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success(revenue))

            val previousRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success(previousRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success(revenue))

            val previousRevenue = givenARevenue(ZERO_VALUE, ZERO_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success(previousRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success(ordersStats))

            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success(ordersStats))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success(ordersStats))

            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success(ordersStats))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue))

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats))

            val currentPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodOrdersStats))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue))

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats))

            val currentPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodOrdersStats))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue))

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats))

            val currentPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodOrdersStats))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue))

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats))

            val currentPeriodOrdersStats = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodOrdersStats))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue))

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(previousPeriodOrdersStats))

            val currentPeriodRevenue = givenRevenueOrderStats(TEN_VALUE.toInt(), TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success(previousPeriodRevenue))

            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null)))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null)))

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success(currentPeriodRevenue))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success(previousPeriodRevenue))

            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null)))

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
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.previousRange), any(), any(), any()))
                .thenReturn(Result.success(previousPeriodRevenue))

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE, null)
            whenever(statsRepository.fetchRevenueStats(eq(testSelectionData.currentRange), any(), any(), any()))
                .thenReturn(Result.success(currentPeriodRevenue))

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
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
                .thenReturn(Result.success(revenue))

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
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
                .thenReturn(Result.success(revenue))

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
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
                .thenReturn(Result.success(revenue))

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
                eq(testSelectionData.previousRange),
                any(),
                any(),

                any()
            )
        }

    @Test
    fun `when get revenue and products data at same time with Saved fetch strategy, then stats repository is used once per period`() =
        runTest {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE, TEN_VALUE.toInt())
            whenever(statsRepository.getRevenueStatsById(any()))
                .thenReturn(Result.success(revenue))

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
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
                .thenReturn(Result.success(revenue))

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
                eq(testSelectionData.previousRange),
                any(),
                any(),
                any()
            )
            verify(statsRepository, times(2)).fetchRevenueStats(
                eq(testSelectionData.currentRange),
                any(),
                any(),
                any()
            )
        }

    @Test
    fun `given fetch bundle stats fails, then result is error`() =
        runTest {
            // Given
            val error = WooError(
                type = WooErrorType.INVALID_RESPONSE,
                original = BaseRequest.GenericErrorType.INVALID_RESPONSE,
                message = "something fails"
            )

            val report = listOf(
                WCProductBundleItemReport(
                    name = "item 1",
                    image = null,
                    itemsSold = 35,
                    netRevenue = 1000.00
                ),
                WCProductBundleItemReport(
                    name = "item 2",
                    image = null,
                    itemsSold = 15,
                    netRevenue = 300.00
                )
            )

            val bundleStatsResponse = WooResult<WCBundleStats>(error)
            val bundleReportResponse = WooResult(report)

            whenever(statsRepository.fetchProductBundlesStats(any(), any(), any())).thenReturn(bundleStatsResponse)
            whenever(statsRepository.fetchBundleReport(any(), any(), any())).thenReturn(bundleReportResponse)
            // When
            val result = sut.fetchProductBundlesStats(testSelectionData)

            // Then
            assertThat(result).isInstanceOf(AnalyticsRepository.BundlesResult.BundlesError::class.java)
        }

    @Test
    fun `given fetch bundle report fails, then result is error`() =
        runTest {
            // Given
            val error = WooError(
                type = WooErrorType.INVALID_RESPONSE,
                original = BaseRequest.GenericErrorType.INVALID_RESPONSE,
                message = "something fails"
            )

            val stats = WCBundleStats(
                itemsSold = 50,
                netRevenue = 1300.00
            )

            val bundleStatsResponse = WooResult(stats)
            val bundleReportResponse = WooResult<List<WCProductBundleItemReport>>(error)

            whenever(statsRepository.fetchProductBundlesStats(any(), any(), any())).thenReturn(bundleStatsResponse)
            whenever(statsRepository.fetchBundleReport(any(), any(), any())).thenReturn(bundleReportResponse)
            // When
            val result = sut.fetchProductBundlesStats(testSelectionData)

            // Then
            assertThat(result).isInstanceOf(AnalyticsRepository.BundlesResult.BundlesError::class.java)
        }

    @Test
    fun `given fetch bundle report succeed, then result is bundle data`() =
        runTest {
            // Given
            val stats = WCBundleStats(
                itemsSold = 50,
                netRevenue = 1300.00
            )
            val report = listOf(
                WCProductBundleItemReport(
                    name = "item 1",
                    image = null,
                    itemsSold = 35,
                    netRevenue = 1000.00
                ),
                WCProductBundleItemReport(
                    name = "item 2",
                    image = null,
                    itemsSold = 15,
                    netRevenue = 300.00
                )
            )

            val bundleStatsResponse = WooResult(stats)
            val bundleReportResponse = WooResult(report)

            whenever(statsRepository.fetchProductBundlesStats(any(), any(), any())).thenReturn(bundleStatsResponse)
            whenever(statsRepository.fetchBundleReport(any(), any(), any())).thenReturn(bundleReportResponse)
            // When
            val result = sut.fetchProductBundlesStats(testSelectionData)

            // Then
            assertThat(result).isInstanceOf(AnalyticsRepository.BundlesResult.BundlesData::class.java)
        }

    @Test
    fun `given fetch gift cards stats succeed, then result is gift card data`() =
        runTest {
            // Given
            val stats = WCGiftCardStats(
                usedValue = 45L,
                netValue = 300.89,
                intervals = emptyList()
            )

            val giftCardsStatsResponse = WooResult(stats)

            whenever(statsRepository.fetchGiftCardStats(any(), any(), any())).thenReturn(giftCardsStatsResponse)

            // When
            val result = sut.fetchGiftCardsStats(testSelectionData)

            // Then
            assertThat(result).isInstanceOf(AnalyticsRepository.GiftCardResult.GiftCardData::class.java)
        }

    @Test
    fun `given fetch gift cards stats fails, then result is gift card error`() =
        runTest {
            // Given
            val error = WooError(
                type = WooErrorType.INVALID_RESPONSE,
                original = BaseRequest.GenericErrorType.INVALID_RESPONSE,
                message = "something fails"
            )

            val giftCardsStatsResponse = WooResult<WCGiftCardStats>(error)

            whenever(statsRepository.fetchGiftCardStats(any(), any(), any())).thenReturn(giftCardsStatsResponse)

            // When
            val result = sut.fetchGiftCardsStats(testSelectionData)

            // Then
            assertThat(result).isInstanceOf(AnalyticsRepository.GiftCardResult.GiftCardError::class.java)
        }

    @Test
    fun `given fetch session stats is call with a last quarter granularity, then visitors stats is called`() =
        runTest {
            val latQuarterRangeSelection = StatsTimeRangeSelection.SelectionType.LAST_QUARTER.generateSelectionData(
                referenceStartDate = "2024-01-01".dayStartFrom(),
                referenceEndDate = "2024-03-31".dayEndFrom(),
                calendar = testCalendar,
                locale = testLocale
            )

            val result = Result.success(mapOf("2024-01-05" to 5))
            whenever(statsRepository.fetchVisitorStats(any(), any(), any())).thenReturn(result)

            sut.fetchVisitorsData(rangeSelection = latQuarterRangeSelection, ForceNew)
            verify(statsRepository).fetchVisitorStats(any(), any(), any())
        }

    @Test
    fun `given fetch session stats is call with a quarter to date granularity, then visitors stats is called`() =
        runTest {
            val quarterToDateRangeSelection =
                StatsTimeRangeSelection.SelectionType.QUARTER_TO_DATE.generateSelectionData(
                    referenceStartDate = "2024-04-01".dayStartFrom(),
                    referenceEndDate = "2024-04-14".dayEndFrom(),
                    calendar = testCalendar,
                    locale = testLocale
                )

            val result = Result.success(mapOf("2024-01-05" to 5))
            whenever(statsRepository.fetchVisitorStats(any(), any(), any())).thenReturn(result)

            sut.fetchVisitorsData(rangeSelection = quarterToDateRangeSelection, ForceNew)
            verify(statsRepository).fetchVisitorStats(any(), any(), any())
        }

    @Test
    fun `given fetch session stats is call with a summary supported granularity, then visitors summary stats is called`() =
        runTest {
            val todayRangeSelection = StatsTimeRangeSelection.SelectionType.TODAY.generateSelectionData(
                referenceStartDate = "2024-04-17".dayStartFrom(),
                referenceEndDate = "2024-04-17".dayEndFrom(),
                calendar = testCalendar,
                locale = testLocale
            )

            val result = Result.success(5)
            whenever(statsRepository.fetchTotalVisitorStats(any(), any(), any())).thenReturn(result)

            sut.fetchVisitorsData(rangeSelection = todayRangeSelection, ForceNew)
            verify(statsRepository).fetchTotalVisitorStats(any(), any(), any())
        }

    @Test
    fun `given fetch session stats is call with a non supported granularity, then result is VisitorsNotSupported`() =
        runTest {
            val todayRangeSelection = StatsTimeRangeSelection.SelectionType.CUSTOM.generateSelectionData(
                referenceStartDate = "2024-01-17".dayStartFrom(),
                referenceEndDate = "2024-04-17".dayEndFrom(),
                calendar = testCalendar,
                locale = testLocale
            )

            val result = sut.fetchVisitorsData(rangeSelection = todayRangeSelection, ForceNew)
            assertThat(result).isInstanceOf(AnalyticsRepository.VisitorsResult.VisitorsNotSupported::class.java)
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
        locale = testLocale
    )

    companion object {
        const val TEN_VALUE = 10.0
        const val ZERO_VALUE = 0.0

        const val ZERO_DELTA = 0
        const val ONE_HUNDRED_DECREASE = -100

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
