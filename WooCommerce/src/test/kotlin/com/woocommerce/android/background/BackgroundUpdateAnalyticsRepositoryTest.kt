package com.woocommerce.android.background

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCBundleStats
import org.wordpress.android.fluxc.model.WCGiftCardStats
import org.wordpress.android.fluxc.model.WCProductBundleItemReport
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.google.WCGoogleAdsProgramTotals
import org.wordpress.android.fluxc.model.google.WCGoogleAdsPrograms
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGoogleStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundUpdateAnalyticsRepositoryTest : BaseUnitTest() {

    private val statsRepository: StatsRepository = mock()
    private val googleAdsStore: WCGoogleStore = mock()
    private val selectedSite: SelectedSite = mock()

    private val testLocale = Locale.US
    private val testTimeZone = TimeZone.getDefault()
    private val testCalendar = Calendar.getInstance(testLocale)

    private val testSelectionData: StatsTimeRangeSelection = CUSTOM.generateSelectionData(
        referenceStartDate = "2024-10-02".dayStartFrom(),
        referenceEndDate = "2024-10-02".dayEndFrom(),
        calendar = testCalendar,
        locale = testLocale
    )

    private val backgroundUpdateAnalyticsRepository = BackgroundUpdateAnalyticsRepository(
        statsRepository,
        googleAdsStore,
        selectedSite
    )

    @Test
    fun `fetchRevenueStats - success`() = testBlocking {
        val mockCurrentResult: Result<WCRevenueStatsModel?> = Result.success(mock())
        val mockPreviousResult: Result<WCRevenueStatsModel?> = Result.success(mock())

        whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
            .thenReturn(mockCurrentResult, mockPreviousResult)

        val result = backgroundUpdateAnalyticsRepository.fetchRevenueStats(testSelectionData)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `fetchRevenueStats - failure from statsRepository`() = testBlocking {
        val mockError = Exception("Error fetching revenue stats")
        val mockCurrentResult: Result<WCRevenueStatsModel?> = Result.failure(mockError)

        whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
            .thenReturn(mockCurrentResult, Result.success(mock()))

        val result = backgroundUpdateAnalyticsRepository.fetchRevenueStats(testSelectionData)

        assertTrue(result.isFailure)
        val multipleErrorsException = result.exceptionOrNull() as MultipleErrorsException
        assertEquals(1, multipleErrorsException.errors.size)
        val wrappedException = multipleErrorsException.errors[0]
        assertEquals(mockError.message, wrappedException.message)
    }

    @Test
    fun `fetchRevenueStats - failure from both calls`() = testBlocking {
        val mockError1 = Exception("Error fetching current revenue stats")
        val mockError2 = Exception("Error fetching previous revenue stats")

        val mockCurrentResult: Result<WCRevenueStatsModel?> = Result.failure(mockError1)
        val mockPreviousResult: Result<WCRevenueStatsModel?> = Result.failure(mockError2)

        whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
            .thenReturn(mockCurrentResult, mockPreviousResult)

        val result = backgroundUpdateAnalyticsRepository.fetchRevenueStats(testSelectionData)

        assertTrue(result.isFailure)
        val multipleErrorsException = result.exceptionOrNull() as MultipleErrorsException
        assertEquals(2, multipleErrorsException.errors.size)
        val wrappedException1 = multipleErrorsException.errors[0]
        assertEquals(mockError1.message, wrappedException1.message)
        val wrappedException2 = multipleErrorsException.errors[1]
        assertEquals(mockError2.message, wrappedException2.message)
    }

    @Test
    fun `fetchGiftCardsStats - success`() = testBlocking {
        val currentStats = WCGiftCardStats(
            usedValue = 100,
            netValue = 100.0,
            intervals = emptyList()
        )
        val previousStats = WCGiftCardStats(
            usedValue = 50,
            netValue = 50.0,
            intervals = emptyList()
        )
        val mockCurrentResult: WooResult<WCGiftCardStats> = WooResult(currentStats)
        val mockPreviousResult: WooResult<WCGiftCardStats> = WooResult(previousStats)

        whenever(statsRepository.fetchGiftCardStats(any(), any(), any()))
            .thenReturn(mockCurrentResult, mockPreviousResult)

        val result = backgroundUpdateAnalyticsRepository.fetchGiftCardsStats(testSelectionData)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `fetchGiftCardsStats - failure from statsRepository`() = testBlocking {
        val mockError = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching current gift card stats"
        )
        val previousStats = WCGiftCardStats(
            usedValue = 50,
            netValue = 50.0,
            intervals = emptyList()
        )
        val mockCurrentResult: WooResult<WCGiftCardStats> = WooResult(mockError)

        whenever(statsRepository.fetchGiftCardStats(startDate = any(), any(), any()))
            .thenReturn(mockCurrentResult, WooResult(previousStats))

        val result = backgroundUpdateAnalyticsRepository.fetchGiftCardsStats(testSelectionData)

        assertTrue(result.isFailure)
        val multipleErrorsException = result.exceptionOrNull() as MultipleErrorsException
        assertEquals(1, multipleErrorsException.errors.size)
    }

    @Test
    fun `fetchGiftCardsStats - failure from both calls`() = testBlocking {
        val mockError1 = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching current gift card stats"
        )
        val mockError2 = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching previous gift card stats"
        )

        val mockCurrentResult: WooResult<WCGiftCardStats> = WooResult(mockError1)
        val mockPreviousResult: WooResult<WCGiftCardStats> = WooResult(mockError2)

        whenever(statsRepository.fetchGiftCardStats(any(), any(), any()))
            .thenReturn(mockCurrentResult, mockPreviousResult)

        val result = backgroundUpdateAnalyticsRepository.fetchGiftCardsStats(testSelectionData)

        assertTrue(result.isFailure)
        val multipleErrorsException = result.exceptionOrNull() as MultipleErrorsException
        assertEquals(2, multipleErrorsException.errors.size)
    }

    @Test
    fun `fetchTopPerformers - success`() = testBlocking {
        val topPerformersResult = Result.success(Unit)

        whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
            .thenReturn(topPerformersResult)

        val result = backgroundUpdateAnalyticsRepository.fetchTopPerformers(testSelectionData)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `fetchTopPerformers - failure`() = testBlocking {
        val error = Exception("Error fetching top performers products")
        val topPerformersResult: Result<Unit> = Result.failure(error)

        whenever(statsRepository.fetchTopPerformerProducts(any(), any(), any(), any()))
            .thenReturn(topPerformersResult)

        val result = backgroundUpdateAnalyticsRepository.fetchTopPerformers(testSelectionData)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as Exception
        assertEquals(error.message, exception.message)
    }

    @Test
    fun `fetchVisitorsStats quarter to date - success`() = testBlocking {
        val quarterSelectionData =
            StatsTimeRangeSelection.SelectionType.QUARTER_TO_DATE.generateSelectionData(
                referenceStartDate = "2024-04-01".dayStartFrom(),
                referenceEndDate = "2024-04-14".dayEndFrom(),
                calendar = testCalendar,
                locale = testLocale
            )

        val visitorsStatsResult = Result.success(mapOf("2024-10-02" to 12))

        whenever(statsRepository.fetchVisitorStats(any(), any(), any()))
            .thenReturn(visitorsStatsResult)

        val result = backgroundUpdateAnalyticsRepository.fetchVisitorsStats(quarterSelectionData)

        assertTrue(result.isSuccess)
        assertEquals(12, result.getOrNull())
        verify(statsRepository, never()).fetchTotalVisitorStats(any(), any(), any())
    }

    @Test
    fun `fetchVisitorsStats last quarter - success`() = testBlocking {
        val quarterSelectionData =
            StatsTimeRangeSelection.SelectionType.LAST_QUARTER.generateSelectionData(
                referenceStartDate = "2024-01-01".dayStartFrom(),
                referenceEndDate = "2024-03-31".dayEndFrom(),
                calendar = testCalendar,
                locale = testLocale
            )

        val visitorsStatsResult = Result.success(mapOf("2024-10-02" to 12))

        whenever(statsRepository.fetchVisitorStats(any(), any(), any()))
            .thenReturn(visitorsStatsResult)

        val result = backgroundUpdateAnalyticsRepository.fetchVisitorsStats(quarterSelectionData)

        assertTrue(result.isSuccess)
        assertEquals(12, result.getOrNull())
        verify(statsRepository, never()).fetchTotalVisitorStats(any(), any(), any())
    }

    @Test
    fun `fetchVisitorsStats - success`() = testBlocking {
        val visitorsStatsResult = Result.success(12)

        whenever(statsRepository.fetchTotalVisitorStats(any(), any(), any()))
            .thenReturn(visitorsStatsResult)

        val result = backgroundUpdateAnalyticsRepository.fetchVisitorsStats(testSelectionData)

        assertTrue(result.isSuccess)
        assertEquals(12, result.getOrNull())
        verify(statsRepository, never()).fetchVisitorStats(any(), any(), any())
    }

    @Test
    fun `fetchVisitorsStats quarter to date - failure`() = testBlocking {
        val quarterSelectionData =
            StatsTimeRangeSelection.SelectionType.QUARTER_TO_DATE.generateSelectionData(
                referenceStartDate = "2024-04-01".dayStartFrom(),
                referenceEndDate = "2024-04-14".dayEndFrom(),
                calendar = testCalendar,
                locale = testLocale
            )

        val exception = Exception("Error fetching visitors stats")
        val visitorsStatsResult: Result<Map<String, Int>> = Result.failure(exception)

        whenever(statsRepository.fetchVisitorStats(any(), any(), any()))
            .thenReturn(visitorsStatsResult)

        val result = backgroundUpdateAnalyticsRepository.fetchVisitorsStats(quarterSelectionData)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(statsRepository, never()).fetchTotalVisitorStats(any(), any(), any())
    }

    @Test
    fun `fetchVisitorsStats last quarter - failure`() = testBlocking {
        val quarterSelectionData =
            StatsTimeRangeSelection.SelectionType.LAST_QUARTER.generateSelectionData(
                referenceStartDate = "2024-01-01".dayStartFrom(),
                referenceEndDate = "2024-03-31".dayEndFrom(),
                calendar = testCalendar,
                locale = testLocale
            )

        val exception = Exception("Error fetching visitors stats")
        val visitorsStatsResult: Result<Map<String, Int>> = Result.failure(exception)

        whenever(statsRepository.fetchVisitorStats(any(), any(), any()))
            .thenReturn(visitorsStatsResult)

        val result = backgroundUpdateAnalyticsRepository.fetchVisitorsStats(quarterSelectionData)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(statsRepository, never()).fetchTotalVisitorStats(any(), any(), any())
    }

    @Test
    fun `fetchVisitorsStats - failure`() = testBlocking {
        val exception = Exception("Error fetching visitors stats")
        val visitorsStatsResult: Result<Int> = Result.failure(exception)

        whenever(statsRepository.fetchTotalVisitorStats(any(), any(), any()))
            .thenReturn(visitorsStatsResult)

        val result = backgroundUpdateAnalyticsRepository.fetchVisitorsStats(testSelectionData)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(statsRepository, never()).fetchVisitorStats(any(), any(), any())
    }

    @Test
    fun `fetchProductBundlesStats - success`() = testBlocking {
        val currentStats = WCBundleStats(
            itemsSold = 14,
            netRevenue = 34.0
        )
        val previousStats = WCBundleStats(
            itemsSold = 14,
            netRevenue = 34.0
        )
        val bundlesReport = listOf(
            WCProductBundleItemReport(
                name = "Item",
                image = "image",
                itemsSold = 14,
                netRevenue = 34.0
            )
        )

        whenever(statsRepository.fetchProductBundlesStats(any(), any(), any()))
            .thenReturn(WooResult(currentStats), WooResult(previousStats))
        whenever(statsRepository.fetchBundleReport(any(), any(), any()))
            .thenReturn(WooResult(bundlesReport))

        val result = backgroundUpdateAnalyticsRepository.fetchProductBundlesStats(testSelectionData)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `fetchProductBundlesStats - failure from previous stats`() = testBlocking {
        val currentStats = WCBundleStats(
            itemsSold = 14,
            netRevenue = 34.0
        )
        val previousStats = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching previous bundle stats"
        )
        val bundlesReport = listOf(
            WCProductBundleItemReport(
                name = "Item",
                image = "image",
                itemsSold = 14,
                netRevenue = 34.0
            )
        )

        whenever(statsRepository.fetchProductBundlesStats(any(), any(), any()))
            .thenReturn(WooResult(currentStats), WooResult(previousStats))
        whenever(statsRepository.fetchBundleReport(any(), any(), any()))
            .thenReturn(WooResult(bundlesReport))

        val result = backgroundUpdateAnalyticsRepository.fetchProductBundlesStats(testSelectionData)

        assertTrue(result.isFailure)
        val multipleErrorsException = result.exceptionOrNull() as MultipleErrorsException
        assertEquals(1, multipleErrorsException.errors.size)
    }

    @Test
    fun `fetchProductBundlesStats - failure from bundle report`() = testBlocking {
        val currentStats = WCBundleStats(
            itemsSold = 14,
            netRevenue = 34.0
        )
        val previousStats = WCBundleStats(
            itemsSold = 14,
            netRevenue = 34.0
        )
        val bundlesReport = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching bundle report"
        )

        whenever(statsRepository.fetchProductBundlesStats(any(), any(), any()))
            .thenReturn(WooResult(currentStats), WooResult(previousStats))
        whenever(statsRepository.fetchBundleReport(any(), any(), any()))
            .thenReturn(WooResult(bundlesReport))

        val result = backgroundUpdateAnalyticsRepository.fetchProductBundlesStats(testSelectionData)

        assertTrue(result.isFailure)
        val multipleErrorsException = result.exceptionOrNull() as MultipleErrorsException
        assertEquals(1, multipleErrorsException.errors.size)
    }

    @Test
    fun `fetchProductBundlesStats - failure from all requests`() = testBlocking {
        val currentStats = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching current bundle stats"
        )
        val previousStats = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching previous bundle stats"
        )
        val bundlesReport = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching bundle report"
        )

        whenever(statsRepository.fetchProductBundlesStats(any(), any(), any()))
            .thenReturn(WooResult(currentStats), WooResult(previousStats))
        whenever(statsRepository.fetchBundleReport(any(), any(), any()))
            .thenReturn(WooResult(bundlesReport))

        val result = backgroundUpdateAnalyticsRepository.fetchProductBundlesStats(testSelectionData)

        assertTrue(result.isFailure)
        val multipleErrorsException = result.exceptionOrNull() as MultipleErrorsException
        assertEquals(3, multipleErrorsException.errors.size)
    }

    @Test
    fun `fetchGoogleAdsStats - success`() = testBlocking {
        val currentStats = WCGoogleAdsPrograms(
            campaigns = emptyList(),
            intervals = emptyList(),
            totals = WCGoogleAdsProgramTotals(
                sales = 0.0,
                spend = 0.0,
                impressions = 0.0,
                clicks = 0.0,
                conversions = 0.0
            )
        )

        val previousStats = WCGoogleAdsPrograms(
            campaigns = emptyList(),
            intervals = emptyList(),
            totals = WCGoogleAdsProgramTotals(
                sales = 0.0,
                spend = 0.0,
                impressions = 0.0,
                clicks = 0.0,
                conversions = 0.0
            )
        )

        whenever(selectedSite.get()).thenReturn(SiteModel())

        whenever(googleAdsStore.fetchAllPrograms(any(), any(), any(), any(), any()))
            .thenReturn(WooResult(currentStats), WooResult(previousStats))

        val result = backgroundUpdateAnalyticsRepository.fetchGoogleAdsStats(testSelectionData)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `fetchGoogleAdsStats - failure from previous stats`() = testBlocking {
        val currentStats = WCGoogleAdsPrograms(
            campaigns = emptyList(),
            intervals = emptyList(),
            totals = WCGoogleAdsProgramTotals(
                sales = 0.0,
                spend = 0.0,
                impressions = 0.0,
                clicks = 0.0,
                conversions = 0.0
            )
        )

        val previousStats = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching previous google ads stats"
        )

        whenever(selectedSite.get()).thenReturn(SiteModel())

        whenever(googleAdsStore.fetchAllPrograms(any(), any(), any(), any(), any()))
            .thenReturn(WooResult(currentStats), WooResult(previousStats))

        val result = backgroundUpdateAnalyticsRepository.fetchGoogleAdsStats(testSelectionData)

        assertTrue(result.isFailure)
        val multipleErrorsException = result.exceptionOrNull() as MultipleErrorsException
        assertEquals(1, multipleErrorsException.errors.size)
    }

    @Test
    fun `fetchGoogleAdsStats - failure from both calls`() = testBlocking {
        val currentStats = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching current google ads stats"
        )

        val previousStats = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Error fetching previous google ads stats"
        )

        whenever(selectedSite.get()).thenReturn(SiteModel())

        whenever(googleAdsStore.fetchAllPrograms(any(), any(), any(), any(), any()))
            .thenReturn(WooResult(currentStats), WooResult(previousStats))

        val result = backgroundUpdateAnalyticsRepository.fetchGoogleAdsStats(testSelectionData)

        assertTrue(result.isFailure)
        val multipleErrorsException = result.exceptionOrNull() as MultipleErrorsException
        assertEquals(2, multipleErrorsException.errors.size)
    }

    private fun String.dayStartFrom(): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = testTimeZone
        val referenceDate = formatter.parse(this)!!
        testCalendar.time = referenceDate
        return testCalendar.startOfCurrentDay()
    }

    private fun String.dayEndFrom(): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = testTimeZone
        val referenceDate = formatter.parse(this)!!
        testCalendar.time = referenceDate
        return testCalendar.endOfCurrentDay()
    }
}
