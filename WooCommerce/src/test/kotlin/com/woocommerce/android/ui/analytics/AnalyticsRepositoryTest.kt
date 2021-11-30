package com.woocommerce.android.ui.analytics

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRanges
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange
import com.woocommerce.android.ui.mystore.StatsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class AnalyticsRepositoryTest : BaseUnitTest() {
    private val statsRepository: StatsRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()

    private val sut: AnalyticsRepository = AnalyticsRepository(statsRepository, selectedSite, wooCommerceStore)

    @Test
    fun `given no previousPeriodRevenue when fetchRevenueData result is null`() = runBlocking {
        // Given
        whenever(statsRepository.fetchRevenueStats(YEARS, true, PREVIOUS_DATE, PREVIOUS_DATE))
            .thenReturn(Result.failure(NullPointerException()))

        // When
        val result = sut.fetchRevenueStatData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

        // Then
        assertNull(result)
    }

    @Test
    fun `given no currentPeriodRevenue when fetchRevenueData result is null`() = runBlocking {
        // Given
        val statsTotal = getStatsTotal(TEN_VALUE, TEN_VALUE)
        whenever(statsRepository.fetchRevenueStats(YEARS, true, PREVIOUS_DATE, PREVIOUS_DATE))
            .thenReturn(Result.success(statsTotal))

        whenever(statsRepository.fetchRevenueStats(YEARS, true, CURRENT_DATE, CURRENT_DATE))
            .thenReturn(Result.failure(NullPointerException()))

        // When
        val result = sut.fetchRevenueStatData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

        // Then
        assertNull(result)
    }

    @Test
    fun `given previous and current period revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val statsTotal = getStatsTotal(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
                .thenReturn(Result.success(statsTotal))

            // When
            val result = sut.fetchRevenueStatData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            assertNotNull(result)
            assertEquals(TEN_VALUE, result.totalValue)
            assertEquals(TEN_VALUE, result.netValue)
            assertEquals(ZERO_DELTA, result.totalDelta)
            assertEquals(ZERO_DELTA, result.netDelta)
        }

    @Test
    fun `given zero previous total revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val previousStats = getStatsTotal(ZERO_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(YEARS, true, PREVIOUS_DATE, PREVIOUS_DATE))
                .thenReturn(Result.success(previousStats))

            val currentStats = getStatsTotal(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(YEARS, true, CURRENT_DATE, CURRENT_DATE))
                .thenReturn(Result.success(currentStats))

            // When
            val result = sut.fetchRevenueStatData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            assertNotNull(result)
            assertEquals(TEN_VALUE, result.totalValue)
            assertEquals(TEN_VALUE, result.netValue)
            assertEquals(THOUSAND_DELTA, result.totalDelta)
            assertEquals(ZERO_DELTA, result.netDelta)
        }

    @Test
    fun `given zero previous net revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val previousStats = getStatsTotal(TEN_VALUE, ZERO_VALUE)
            whenever(statsRepository.fetchRevenueStats(YEARS, true, PREVIOUS_DATE, PREVIOUS_DATE))
                .thenReturn(Result.success(previousStats))

            val currentStats = getStatsTotal(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(YEARS, true, CURRENT_DATE, CURRENT_DATE))
                .thenReturn(Result.success(currentStats))

            // When
            val result = sut.fetchRevenueStatData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            assertNotNull(result)
            assertEquals(TEN_VALUE, result.totalValue)
            assertEquals(TEN_VALUE, result.netValue)
            assertEquals(ZERO_DELTA, result.totalDelta)
            assertEquals(THOUSAND_DELTA, result.netDelta)
        }

    @Test
    fun `given null previous total revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val previousStats = getStatsTotal(null, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(YEARS, true, PREVIOUS_DATE, PREVIOUS_DATE))
                .thenReturn(Result.success(previousStats))

            val currentStats = getStatsTotal(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(YEARS, true, CURRENT_DATE, CURRENT_DATE))
                .thenReturn(Result.success(currentStats))

            // When
            val result = sut.fetchRevenueStatData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            assertNotNull(result)
            assertEquals(TEN_VALUE, result.totalValue)
            assertEquals(TEN_VALUE, result.netValue)
            assertEquals(THOUSAND_DELTA, result.totalDelta)
            assertEquals(ZERO_DELTA, result.netDelta)
        }

    @Test
    fun `given null previous net revenue when fetchRevenueData result is the expected`() =
        runBlocking {
            // Given
            val previousStats = getStatsTotal(TEN_VALUE, null)
            whenever(statsRepository.fetchRevenueStats(YEARS, true, PREVIOUS_DATE, PREVIOUS_DATE))
                .thenReturn(Result.success(previousStats))

            val currentStats = getStatsTotal(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(YEARS, true, CURRENT_DATE, CURRENT_DATE))
                .thenReturn(Result.success(currentStats))

            // When
            val result = sut.fetchRevenueStatData(DateRange.SimpleDateRange(previousDate!!, currentDate!!), ANY_RANGE)

            // Then
            assertNotNull(result)
            assertEquals(TEN_VALUE, result.totalValue)
            assertEquals(TEN_VALUE, result.netValue)
            assertEquals(ZERO_DELTA, result.totalDelta)
            assertEquals(THOUSAND_DELTA, result.netDelta)
        }

    @Test
    fun `given previous and current period revenue when fetchRevenueData multiple date range result is the expected`() =
        runBlocking {
            // Given
            val statsTotal = getStatsTotal(TEN_VALUE, TEN_VALUE)
            whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
                .thenReturn(Result.success(statsTotal))

            // When
            val result = sut.fetchRevenueStatData(DateRange.MultipleDateRange(
                DateRange.SimpleDateRange(previousDate!!, previousDate),
                DateRange.SimpleDateRange(currentDate!!, currentDate)
            ), ANY_RANGE)

            // Then
            assertNotNull(result)
            assertEquals(TEN_VALUE, result.totalValue)
            assertEquals(TEN_VALUE, result.netValue)
            assertEquals(ZERO_DELTA, result.totalDelta)
            assertEquals(ZERO_DELTA, result.netDelta)
        }

    private fun getStatsTotal(totalValue: Double?, netValue: Double?): WCRevenueStatsModel {
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

        val ANY_RANGE = AnalyticsDateRanges.LAST_YEAR
        private val sdf = SimpleDateFormat("yyyy-MM-dd")
        val previousDate: Date? = sdf.parse(PREVIOUS_DATE)
        val currentDate: Date? = sdf.parse(CURRENT_DATE)
    }
}
