package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository.FetchStrategy.Saved
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository.OrdersResult
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository.OrdersResult.OrdersData
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository.ProductsResult
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository.ProductsResult.ProductsData
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository.RevenueResult
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository.VisitorsResult
import com.woocommerce.android.ui.analytics.sync.AnalyticsRepository.VisitorsResult.VisitorsData
import com.woocommerce.android.ui.analytics.sync.OrdersState
import com.woocommerce.android.ui.analytics.sync.UpdateAnalyticsHubStats
import com.woocommerce.android.viewmodel.BaseUnitTest
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
internal class UpdateAnalyticsHubStatsTest : BaseUnitTest() {
    private lateinit var repository: AnalyticsRepository

    private lateinit var sut: UpdateAnalyticsHubStats

    @Before
    fun setUp() {
        repository = mock()
        sut = UpdateAnalyticsHubStats(
            analyticsRepository = repository
        )
    }

    @Test
    fun `initial test`() = testBlocking {
        // Given
        configureSuccessResponseStub()
        val orderStatsUpdates = mutableListOf<OrdersState>()
        val job = sut.ordersState
            .onEach { orderStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, Saved)

        advanceUntilIdle()

        // Then
        assertThat(orderStatsUpdates).isNotEmpty

        job.cancel()
    }

    private fun configureSuccessResponseStub() {
        repository.stub {
            onBlocking {
                repository.fetchRevenueData(testRangeSelection, Saved)
            } doReturn testRevenueResult

            onBlocking {
                repository.fetchOrdersData(testRangeSelection, Saved)
            } doReturn testOrdersResult

            onBlocking {
                repository.fetchProductsData(testRangeSelection, Saved)
            } doReturn testProductsResult

            onBlocking {
                repository.fetchVisitorsData(testRangeSelection, Saved)
            } doReturn testVisitorsResult
        }
    }

    private val testRangeSelection = SelectionType.TODAY.generateSelectionData(
        calendar = Calendar.getInstance(),
        locale = Locale.getDefault()
    )

    private val testRevenueResult = RevenueData(RevenueStat.EMPTY) as RevenueResult
    private val testOrdersResult = OrdersData(OrdersStat.EMPTY) as OrdersResult
    private val testProductsResult = ProductsData(ProductsStat.EMPTY) as ProductsResult
    private val testVisitorsResult = VisitorsData(0) as VisitorsResult
}
