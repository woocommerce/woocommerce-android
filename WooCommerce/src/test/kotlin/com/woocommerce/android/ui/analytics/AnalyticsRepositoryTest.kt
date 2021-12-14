package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.AnalyticsRepository.Companion.ANALYTICS_REVENUE_PATH
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.MultipleDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.SimpleDateRange
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AnalyticsRepositoryTest : BaseUnitTest() {
    private val statsRepository: StatsRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()

    private val sut: AnalyticsRepository = AnalyticsRepository(statsRepository, selectedSite, wooCommerceStore)

    @Test
    fun `given no currentPeriodRevenue, when fetchRevenueData, then result is RevenueError`() = runBlocking {
        // Given
        val previousPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
            .thenReturn(listOf(Result.success(previousPeriodRevenue)).asFlow())

        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
            .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

        // When
        val result = sut.fetchRevenueData(SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

        // Then
        with(result.first()) {
            assertTrue(this is RevenueError)
        }
    }

    @Test
    fun `given no previousRevenuePeriod, when fetchRevenueData, then result is RevenueError`() = runBlocking {
        // Given
        val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
            .thenReturn(listOf(Result.success(currentPeriodRevenue)).asFlow())

        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
            .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

        // When
        val result = sut.fetchRevenueData(SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

        // Then
        with(result.first()) {
            assertTrue(this is RevenueError)
        }
    }

    @Test
    fun `given previous and current period revenue, when fetchRevenueData, then result is the expected`() =
        runBlocking {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                SimpleDateRange(previousDate!!, currentDate!!),
                ANY_RANGE
            )

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
                assertTrue(revenueStat.totalDelta is DeltaPercentage.Value)
                assertTrue(revenueStat.netDelta is DeltaPercentage.Value)
            }
        }

    @Test
    fun `given previous revenue and current zero revenue, when fetchRevenueData, then deltas are the expected`() =
        runBlocking {
            // Given
            val revenue = givenARevenue(ZERO_VALUE, ZERO_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            val previousRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success(previousRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertTrue(this is RevenueData)
                assertTrue(revenueStat.totalDelta is DeltaPercentage.Value)
                assertEquals(ONE_HUNDRED_DECREASE, (revenueStat.totalDelta as DeltaPercentage.Value).value)
                assertTrue(revenueStat.netDelta is DeltaPercentage.Value)
                assertEquals(ONE_HUNDRED_DECREASE, (revenueStat.netDelta as DeltaPercentage.Value).value)
            }
        }

    @Test
    fun `given zero previous and current revenue, when fetchRevenueData, then deltas are the expected`() =
        runBlocking {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            val previousRevenue = givenARevenue(ZERO_VALUE, ZERO_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success(previousRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertTrue(this is RevenueData)
                assertTrue(revenueStat.totalDelta is DeltaPercentage.NotExist)
                assertTrue(revenueStat.netDelta is DeltaPercentage.NotExist)
            }
        }

    @Test
    fun `given zero previous total revenue, when fetchRevenueData, then result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(ZERO_VALUE, ZERO_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
            }
        }

    @Test
    fun `given zero previous net revenue, when fetchRevenueData, then result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, ZERO_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
            }
        }

    @Test
    fun `given null previous total revenue, when fetchRevenueData, then result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(null, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
            }
        }

    @Test
    fun `given null previous net revenue,  when fetchRevenueData, then result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, null)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
            }
        }

    @Test
    fun `given previous and current revenue, when fetchRevenueData multiple date range, then result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(
                MultipleDateRange(
                    SimpleDateRange(previousDate!!, previousDate),
                    SimpleDateRange(currentDate!!, currentDate)
                ),
                ANY_RANGE
            )

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
            }
        }

    @Test
    fun `when get revenue admin url panel, then is expected`() {
        val siteModel: SiteModel = mock()
        whenever(siteModel.adminUrl).thenReturn(ANY_URL)
        whenever(selectedSite.getIfExists()).thenReturn(siteModel)

        val adminPanelUrl = sut.getRevenueAdminPanelUrl()

        assertEquals(ANY_URL + ANALYTICS_REVENUE_PATH, adminPanelUrl)
    }

    private fun givenARevenue(totalSales: Double?, netValue: Double?): WCRevenueStatsModel {
        val stats: WCRevenueStatsModel = mock()
        val revenueStatsTotal: WCRevenueStatsModel.Total = mock()
        whenever(revenueStatsTotal.totalSales).thenReturn(totalSales)
        whenever(revenueStatsTotal.netRevenue).thenReturn(netValue)
        whenever(stats.parseTotal()).thenReturn(revenueStatsTotal)
        return stats
    }

    companion object {
        const val PREVIOUS_DATE = "2021-01-01"
        const val CURRENT_DATE = "2021-01-02"

        const val TEN_VALUE = 10.0
        const val ZERO_VALUE = 0.0

        const val ONE_HUNDRED_DECREASE = -100

        const val ANY_URL = "https://a8c.com"

        val ANY_RANGE = AnalyticTimePeriod.LAST_YEAR
        private val sdf = SimpleDateFormat("yyyy-MM-dd")
        val previousDate: Date? = sdf.parse(PREVIOUS_DATE)
        val currentDate: Date? = sdf.parse(CURRENT_DATE)
    }
}
