package com.woocommerce.android.background

import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateAnalyticsDataByRangeSelectionTest : BaseUnitTest() {

    private val analyticsCardsConfiguration: ObserveAnalyticsCardsConfiguration = mock()
    private val backgroundUpdateAnalyticsRepository: BackgroundUpdateAnalyticsRepository = mock()
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore = mock()

    private val defaultVisibleCards = listOf(
        AnalyticCardConfiguration(
            card = AnalyticsCards.Bundles,
            title = AnalyticsCards.Bundles.name,
            isVisible = true,
        ),
        AnalyticCardConfiguration(
            card = AnalyticsCards.GiftCards,
            title = AnalyticsCards.GiftCards.name,
            isVisible = true,
        ),
        AnalyticCardConfiguration(
            card = AnalyticsCards.Revenue,
            title = AnalyticsCards.Revenue.name,
            isVisible = true,
        ),
        AnalyticCardConfiguration(
            card = AnalyticsCards.Orders,
            title = AnalyticsCards.Orders.name,
            isVisible = true,
        ),
        AnalyticCardConfiguration(
            card = AnalyticsCards.Products,
            title = AnalyticsCards.Products.name,
            isVisible = false,
        ),
        AnalyticCardConfiguration(
            card = AnalyticsCards.Session,
            title = AnalyticsCards.Session.name,
            isVisible = false,
        )
    )

    private val sut = UpdateAnalyticsDataByRangeSelection(
        analyticsCardsConfiguration = analyticsCardsConfiguration,
        backgroundUpdateAnalyticsRepository = backgroundUpdateAnalyticsRepository,
        analyticsUpdateDataStore = analyticsUpdateDataStore
    )

    private val defaultRangeSelection = StatsTimeRangeSelection.SelectionType.TODAY.generateSelectionData(
        calendar = Calendar.getInstance(),
        locale = Locale.getDefault(),
        referenceStartDate = Date(),
        referenceEndDate = Date()
    )

    @Test
    fun `when there are visible cards then refresh visible and forced cards data`() = runTest {
        whenever(analyticsCardsConfiguration.invoke()).doReturn(flowOf(defaultVisibleCards))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchRevenueStats(defaultRangeSelection)
        ).doReturn(Result.success(Unit))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchTopPerformers(defaultRangeSelection)
        ).doReturn(Result.success(Unit))

        sut.invoke(defaultRangeSelection, listOf(AnalyticsCards.Products))

        verify(backgroundUpdateAnalyticsRepository).fetchRevenueStats(defaultRangeSelection)
        verify(backgroundUpdateAnalyticsRepository).fetchTopPerformers(defaultRangeSelection)

        verify(backgroundUpdateAnalyticsRepository, never()).fetchVisitorsStats(defaultRangeSelection)
    }

    @Test
    fun `when there are NO visible cards then refresh only forced cards data`() = runTest {
        whenever(analyticsCardsConfiguration.invoke()).doReturn(flowOf(emptyList()))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchRevenueStats(defaultRangeSelection)
        ).doReturn(Result.success(Unit))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchTopPerformers(defaultRangeSelection)
        ).doReturn(Result.success(Unit))

        sut.invoke(defaultRangeSelection, listOf(AnalyticsCards.Products))

        verify(backgroundUpdateAnalyticsRepository).fetchRevenueStats(defaultRangeSelection)
        verify(backgroundUpdateAnalyticsRepository).fetchTopPerformers(defaultRangeSelection)

        verify(backgroundUpdateAnalyticsRepository, never()).fetchVisitorsStats(defaultRangeSelection)
    }

    @Test
    fun `when one request fails then return false`() = runTest {
        val exception = Exception("Error fetching data")
        whenever(analyticsCardsConfiguration.invoke()).doReturn(flowOf(defaultVisibleCards))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchRevenueStats(defaultRangeSelection)
        ).doReturn(Result.success(Unit))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchTopPerformers(defaultRangeSelection)
        ).doReturn(Result.failure(exception))

        val result = sut.invoke(defaultRangeSelection, listOf(AnalyticsCards.Products))

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertEquals(result.exceptionOrNull(), exception)
    }

    @Test
    fun `when all request succeed then return true`() = runTest {
        whenever(analyticsCardsConfiguration.invoke()).doReturn(flowOf(defaultVisibleCards))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchRevenueStats(defaultRangeSelection)
        ).doReturn(Result.success(Unit))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchTopPerformers(defaultRangeSelection)
        ).doReturn(Result.success(Unit))

        val result = sut.invoke(defaultRangeSelection, listOf(AnalyticsCards.Products))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `save last update only for success requests`() = runTest {
        whenever(analyticsCardsConfiguration.invoke()).doReturn(flowOf(defaultVisibleCards))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchRevenueStats(defaultRangeSelection)
        ).doReturn(Result.success(Unit))
        whenever(
            backgroundUpdateAnalyticsRepository.fetchTopPerformers(defaultRangeSelection)
        ).doReturn(Result.failure(Exception("Error fetching data")))

        val result = sut.invoke(defaultRangeSelection, listOf(AnalyticsCards.Products))

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)

        verify(analyticsUpdateDataStore).storeLastAnalyticsUpdate(
            defaultRangeSelection,
            AnalyticsUpdateDataStore.AnalyticData.REVENUE
        )
        verify(analyticsUpdateDataStore).storeLastAnalyticsUpdate(
            defaultRangeSelection,
            AnalyticsUpdateDataStore.AnalyticData.ORDERS
        )
        verify(analyticsUpdateDataStore, never()).storeLastAnalyticsUpdate(
            defaultRangeSelection,
            AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS
        )
    }
}
