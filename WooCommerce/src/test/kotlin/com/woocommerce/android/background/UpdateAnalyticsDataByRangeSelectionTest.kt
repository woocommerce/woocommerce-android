package com.woocommerce.android.background

import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository
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
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateAnalyticsDataByRangeSelectionTest : BaseUnitTest() {

    private val analyticsCardsConfiguration: ObserveAnalyticsCardsConfiguration = mock()
    private val analyticsRepository: AnalyticsRepository = mock()

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
        analyticsRepository = analyticsRepository
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
            analyticsRepository.fetchRevenueData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.RevenueResult.RevenueData(mock()))
        whenever(analyticsRepository.fetchGiftCardsStats(defaultRangeSelection))
            .doReturn(AnalyticsRepository.GiftCardResult.GiftCardData(mock()))
        whenever(analyticsRepository.fetchProductBundlesStats(defaultRangeSelection))
            .doReturn(AnalyticsRepository.BundlesResult.BundlesData(mock()))
        whenever(
            analyticsRepository.fetchOrdersData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.OrdersResult.OrdersData(mock()))
        whenever(
            analyticsRepository.fetchProductsData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.ProductsResult.ProductsData(mock()))

        sut.invoke(defaultRangeSelection, listOf(AnalyticsCards.Products))

        verify(analyticsRepository).fetchRevenueData(
            defaultRangeSelection,
            AnalyticsRepository.FetchStrategy.ForceNew
        )
        verify(analyticsRepository).fetchGiftCardsStats(defaultRangeSelection)
        verify(analyticsRepository).fetchProductBundlesStats(defaultRangeSelection)
        verify(analyticsRepository).fetchOrdersData(
            defaultRangeSelection,
            AnalyticsRepository.FetchStrategy.ForceNew
        )
        verify(analyticsRepository).fetchProductsData(
            defaultRangeSelection,
            AnalyticsRepository.FetchStrategy.ForceNew
        )
        verify(analyticsRepository, never()).fetchVisitorsData(
            defaultRangeSelection,
            AnalyticsRepository.FetchStrategy.ForceNew
        )
    }

    @Test
    fun `when there are NO visible cards then refresh only forced cards data`() = runTest {
        whenever(analyticsCardsConfiguration.invoke()).doReturn(flowOf(emptyList()))
        whenever(
            analyticsRepository.fetchProductsData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.ProductsResult.ProductsData(mock()))

        sut.invoke(defaultRangeSelection, listOf(AnalyticsCards.Products))

        verify(analyticsRepository, never()).fetchRevenueData(
            defaultRangeSelection,
            AnalyticsRepository.FetchStrategy.ForceNew
        )
        verify(analyticsRepository, never()).fetchGiftCardsStats(defaultRangeSelection)
        verify(analyticsRepository, never()).fetchProductBundlesStats(defaultRangeSelection)
        verify(analyticsRepository, never()).fetchOrdersData(
            defaultRangeSelection,
            AnalyticsRepository.FetchStrategy.ForceNew
        )
        verify(analyticsRepository, never()).fetchVisitorsData(
            defaultRangeSelection,
            AnalyticsRepository.FetchStrategy.ForceNew
        )

        verify(analyticsRepository).fetchProductsData(
            defaultRangeSelection,
            AnalyticsRepository.FetchStrategy.ForceNew
        )
    }

    @Test
    fun `when one request fails then return false`() = runTest {
        whenever(analyticsCardsConfiguration.invoke()).doReturn(flowOf(defaultVisibleCards))
        whenever(
            analyticsRepository.fetchRevenueData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.RevenueResult.RevenueData(mock()))
        whenever(analyticsRepository.fetchGiftCardsStats(defaultRangeSelection))
            .doReturn(AnalyticsRepository.GiftCardResult.GiftCardData(mock()))
        whenever(analyticsRepository.fetchProductBundlesStats(defaultRangeSelection))
            .doReturn(AnalyticsRepository.BundlesResult.BundlesData(mock()))
        whenever(
            analyticsRepository.fetchOrdersData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.OrdersResult.OrdersError)
        whenever(
            analyticsRepository.fetchProductsData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.ProductsResult.ProductsData(mock()))

        val result = sut.invoke(defaultRangeSelection, listOf(AnalyticsCards.Products))

        assertFalse(result)
    }

    @Test
    fun `when all request succeed then return true`() = runTest {
        whenever(analyticsCardsConfiguration.invoke()).doReturn(flowOf(defaultVisibleCards))
        whenever(
            analyticsRepository.fetchRevenueData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.RevenueResult.RevenueData(mock()))
        whenever(analyticsRepository.fetchGiftCardsStats(defaultRangeSelection))
            .doReturn(AnalyticsRepository.GiftCardResult.GiftCardData(mock()))
        whenever(analyticsRepository.fetchProductBundlesStats(defaultRangeSelection))
            .doReturn(AnalyticsRepository.BundlesResult.BundlesData(mock()))
        whenever(
            analyticsRepository.fetchOrdersData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.OrdersResult.OrdersData(mock()))
        whenever(
            analyticsRepository.fetchProductsData(
                defaultRangeSelection,
                AnalyticsRepository.FetchStrategy.ForceNew
            )
        ).doReturn(AnalyticsRepository.ProductsResult.ProductsData(mock()))

        val result = sut.invoke(defaultRangeSelection, listOf(AnalyticsCards.Products))

        assertTrue(result)
    }
}
