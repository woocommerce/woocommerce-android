package com.woocommerce.android.ui.analytics

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.AnalyticsRepository.Companion.ANALYTICS_REVENUE_PATH
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRanges
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange
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
    fun `given no currentPeriodRevenue when fetchRevenueData result is RevenueError`() = runBlocking {
        // Given
        val previousPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
            .thenReturn(listOf(Result.success(previousPeriodRevenue)).asFlow())

        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
            .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

        // When
        val result = sut.fetchRevenueData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

        // Then
        with(result.first()) {
            assertTrue(this is RevenueError)
        }
    }

    @Test
    fun `given no previousRevenuePeriod when fetchRevenueData result is the expected`() = runBlocking {
        // Given
        val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
            .thenReturn(listOf(Result.success(currentPeriodRevenue)).asFlow())

        whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
            .thenReturn(listOf(Result.failure<WCRevenueStatsModel?>(StatsRepository.StatsException(null))).asFlow())

        // When
        val result = sut.fetchRevenueData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

        // Then
        with(result.single()) {
            assertNotNull(this)
            assertTrue(this is RevenueData)
            assertEquals(TEN_VALUE, revenueStat.totalValue)
            assertEquals(TEN_VALUE, revenueStat.netValue)
            assertEquals(THOUSAND_DELTA, revenueStat.totalDelta)
            assertEquals(THOUSAND_DELTA, revenueStat.netDelta)
        }
    }

    @Test
    fun `given previous and current period revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val revenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success(revenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
                assertEquals(ZERO_DELTA, revenueStat.totalDelta)
                assertEquals(ZERO_DELTA, revenueStat.netDelta)
            }
        }

    @Test
    fun `given zero previous total revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(ZERO_VALUE, ZERO_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
                assertEquals(THOUSAND_DELTA, revenueStat.totalDelta)
                assertEquals(THOUSAND_DELTA, revenueStat.netDelta)
            }
        }

    @Test
    fun `given zero previous net revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, ZERO_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
                assertEquals(ZERO_DELTA, revenueStat.totalDelta)
                assertEquals(THOUSAND_DELTA, revenueStat.netDelta)
            }
        }

    @Test
    fun `given null previous total revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(null, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
                assertEquals(THOUSAND_DELTA, revenueStat.totalDelta)
                assertEquals(ZERO_DELTA, revenueStat.netDelta)
            }
        }

    @Test
    fun `given null previous net revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, null)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())

            // When
            val result = sut.fetchRevenueData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
                assertEquals(ZERO_DELTA, revenueStat.totalDelta)
                assertEquals(THOUSAND_DELTA, revenueStat.netDelta)
            }
        }

    @Test
    fun `given previous and current period revenue when fetchRevenueData multiple date range result is the expected`() =
        runBlocking {
            // Given
            val previousPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(PREVIOUS_DATE), eq(PREVIOUS_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(previousPeriodRevenue)).asFlow())

            val currentPeriodRevenue = givenARevenue(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), eq(CURRENT_DATE), eq(CURRENT_DATE)))
                .thenReturn(listOf(Result.success<WCRevenueStatsModel?>(currentPeriodRevenue)).asFlow())


            // When
            val result = sut.fetchRevenueData(DateRange.MultipleDateRange(
                DateRange.SimpleDateRange(previousDate!!, previousDate),
                DateRange.SimpleDateRange(currentDate!!, currentDate)
            ), ANY_RANGE)

            // Then
            with(result.single()) {
                assertNotNull(this)
                assertTrue(this is RevenueData)
                assertEquals(TEN_VALUE, revenueStat.totalValue)
                assertEquals(TEN_VALUE, revenueStat.netValue)
                assertEquals(ZERO_DELTA, revenueStat.totalDelta)
                assertEquals(ZERO_DELTA, revenueStat.netDelta)
            }
        }

    @Test
    fun `get admin url panel is expected`() {
        val siteModel: SiteModel = mock()
        whenever(siteModel.adminUrl).thenReturn(ANY_URL)
        whenever(selectedSite.getIfExists()).thenReturn(siteModel)

        val adminPanelUrl = sut.getRevenueAdminPanelUrl()

        assertEquals(ANY_URL + ANALYTICS_REVENUE_PATH, adminPanelUrl)
    }

    private fun givenARevenue(totalValue: Double?, netValue: Double?): WCRevenueStatsModel {
        val stats: WCRevenueStatsModel = mock()
        val revenueStatsTotal: WCRevenueStatsModel.Total = mock()
        whenever(revenueStatsTotal.totalSales).thenReturn(totalValue)
        whenever(revenueStatsTotal.netRevenue).thenReturn(netValue)
        whenever(stats.parseTotal()).thenReturn(revenueStatsTotal)
        return stats
    }

    companion object {
        const val PREVIOUS_DATE = "2021-01-01"
        const val CURRENT_DATE = "2021-01-02"

        const val TEN_VALUE = 10.0
        const val ZERO_VALUE = 0.0

        const val ZERO_DELTA = 0
        const val THOUSAND_DELTA = 1000

        const val ANY_URL = "https://a8c.com"

        val ANY_RANGE = AnalyticsDateRanges.LAST_YEAR
        private val sdf = SimpleDateFormat("yyyy-MM-dd")
        val previousDate: Date? = sdf.parse(PREVIOUS_DATE)
        val currentDate: Date? = sdf.parse(CURRENT_DATE)
    }
}
