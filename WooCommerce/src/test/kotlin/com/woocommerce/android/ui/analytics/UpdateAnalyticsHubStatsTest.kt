package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.FetchStrategy.ForceNew
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.OrdersResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.ProductsResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.RevenueResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.VisitorsResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.hub.sync.OrdersState
import com.woocommerce.android.ui.analytics.hub.sync.ProductsState
import com.woocommerce.android.ui.analytics.hub.sync.RevenueState
import com.woocommerce.android.ui.analytics.hub.sync.SessionState
import com.woocommerce.android.ui.analytics.hub.sync.UpdateAnalyticsHubStats
import com.woocommerce.android.viewmodel.BaseUnitTest
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
    private lateinit var analyticsDataStore: AnalyticsUpdateDataStore
    private lateinit var repository: AnalyticsRepository

    private lateinit var sut: UpdateAnalyticsHubStats

    @Before
    fun setUp() {
        analyticsDataStore = mock {
            onBlocking { shouldUpdateAnalytics(testRangeSelection) } doReturn true
        }
        repository = mock()
        sut = UpdateAnalyticsHubStats(
            analyticsUpdateDataStore = analyticsDataStore,
            analyticsRepository = repository
        )
    }

    @Test
    fun `when syncing stats data successfully, then update the orders with the expected states`() = testBlocking {
        // Given
        configureSuccessResponseStub()
        val orderStatsUpdates = mutableListOf<OrdersState>()
        val job = sut.ordersState
            .onEach { orderStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(orderStatsUpdates).hasSize(3)
        assertThat(orderStatsUpdates[0]).isEqualTo(OrdersState.Available(OrdersStat.EMPTY))
        assertThat(orderStatsUpdates[1]).isEqualTo(OrdersState.Loading)
        assertThat(orderStatsUpdates[2]).isEqualTo(OrdersState.Available(testOrdersStat))

        job.cancel()
    }

    @Test
    fun `when syncing stats data fails, then update the orders with the expected states`() = testBlocking {
        // Given
        configureErrorResponseStub()
        val orderStatsUpdates = mutableListOf<OrdersState>()
        val job = sut.ordersState
            .onEach { orderStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(orderStatsUpdates).hasSize(3)
        assertThat(orderStatsUpdates[0]).isEqualTo(OrdersState.Available(OrdersStat.EMPTY))
        assertThat(orderStatsUpdates[1]).isEqualTo(OrdersState.Loading)
        assertThat(orderStatsUpdates[2]).isEqualTo(OrdersState.Error)

        job.cancel()
    }

    @Test
    fun `when syncing stats data successfully, then update the revenue with the expected states`() = testBlocking {
        // Given
        configureSuccessResponseStub()
        val revenueStatsUpdates = mutableListOf<RevenueState>()
        val job = sut.revenueState
            .onEach { revenueStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(revenueStatsUpdates).hasSize(3)
        assertThat(revenueStatsUpdates[0]).isEqualTo(RevenueState.Available(RevenueStat.EMPTY))
        assertThat(revenueStatsUpdates[1]).isEqualTo(RevenueState.Loading)
        assertThat(revenueStatsUpdates[2]).isEqualTo(RevenueState.Available(testRevenueStat))

        job.cancel()
    }

    @Test
    fun `when syncing stats data fails, then update the revenue with the expected states`() = testBlocking {
        // Given
        configureErrorResponseStub()
        val revenueStatsUpdates = mutableListOf<RevenueState>()
        val job = sut.revenueState
            .onEach { revenueStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(revenueStatsUpdates).hasSize(3)
        assertThat(revenueStatsUpdates[0]).isEqualTo(RevenueState.Available(RevenueStat.EMPTY))
        assertThat(revenueStatsUpdates[1]).isEqualTo(RevenueState.Loading)
        assertThat(revenueStatsUpdates[2]).isEqualTo(RevenueState.Error)

        job.cancel()
    }

    @Test
    fun `when syncing stats data successfully, then update the product with the expected states`() = testBlocking {
        // Given
        configureSuccessResponseStub()
        val productStatsUpdates = mutableListOf<ProductsState>()
        val job = sut.productsState
            .onEach { productStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(productStatsUpdates).hasSize(3)
        assertThat(productStatsUpdates[0]).isEqualTo(ProductsState.Available(ProductsStat.EMPTY))
        assertThat(productStatsUpdates[1]).isEqualTo(ProductsState.Loading)
        assertThat(productStatsUpdates[2]).isEqualTo(ProductsState.Available(testProductsStat))

        job.cancel()
    }

    @Test
    fun `when syncing stats data fails, then update the product with the expected states`() = testBlocking {
        // Given
        configureErrorResponseStub()
        val productStatsUpdates = mutableListOf<ProductsState>()
        val job = sut.productsState
            .onEach { productStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(productStatsUpdates).hasSize(3)
        assertThat(productStatsUpdates[0]).isEqualTo(ProductsState.Available(ProductsStat.EMPTY))
        assertThat(productStatsUpdates[1]).isEqualTo(ProductsState.Loading)
        assertThat(productStatsUpdates[2]).isEqualTo(ProductsState.Error)

        job.cancel()
    }

    @Test
    fun `when syncing stats data successfully, then update the session with the expected states`() = testBlocking {
        // Given
        configureSuccessResponseStub()
        val expectedSessionStat = SessionStat(
            ordersCount = testOrdersStat.ordersCount,
            visitorsCount = testVisitorsCount
        )
        val sessionStatsUpdates = mutableListOf<SessionState>()
        val job = sut.sessionState
            .onEach { sessionStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(sessionStatsUpdates).hasSize(3)
        assertThat(sessionStatsUpdates[0]).isEqualTo(SessionState.Available(SessionStat.EMPTY))
        assertThat(sessionStatsUpdates[1]).isEqualTo(SessionState.Loading)
        assertThat(sessionStatsUpdates[2]).isEqualTo(SessionState.Available(expectedSessionStat))

        job.cancel()
    }

    @Test
    fun `when syncing stats data fails, then update the session with the expected states`() = testBlocking {
        // Given
        configureErrorResponseStub()
        val sessionStatsUpdates = mutableListOf<SessionState>()
        val job = sut.sessionState
            .onEach { sessionStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(sessionStatsUpdates).hasSize(3)
        assertThat(sessionStatsUpdates[0]).isEqualTo(SessionState.Available(SessionStat.EMPTY))
        assertThat(sessionStatsUpdates[1]).isEqualTo(SessionState.Loading)
        assertThat(sessionStatsUpdates[2]).isEqualTo(SessionState.Error)

        job.cancel()
    }

    @Test
    fun `when syncing stats data starts, then store the expected timestamp`() = testBlocking {
    }

    @Test
    fun `when syncing stats data starts under the cache interval, then request data from cache`() = testBlocking {
    }

    @Test
    fun `when syncing stats data starts outside the cache interval, then request data from network`() = testBlocking {
    }

    private fun configureSuccessResponseStub() {
        repository.stub {
            onBlocking {
                repository.fetchRevenueData(testRangeSelection, ForceNew)
            } doReturn testRevenueResult

            onBlocking {
                repository.fetchOrdersData(testRangeSelection, ForceNew)
            } doReturn testOrdersResult

            onBlocking {
                repository.fetchProductsData(testRangeSelection, ForceNew)
            } doReturn testProductsResult

            onBlocking {
                repository.fetchVisitorsData(testRangeSelection, ForceNew)
            } doReturn testVisitorsResult
        }
    }

    private fun configureErrorResponseStub() {
        repository.stub {
            onBlocking {
                repository.fetchRevenueData(testRangeSelection, ForceNew)
            } doReturn RevenueResult.RevenueError

            onBlocking {
                repository.fetchOrdersData(testRangeSelection, ForceNew)
            } doReturn OrdersResult.OrdersError

            onBlocking {
                repository.fetchProductsData(testRangeSelection, ForceNew)
            } doReturn ProductsResult.ProductsError

            onBlocking {
                repository.fetchVisitorsData(testRangeSelection, ForceNew)
            } doReturn VisitorsResult.VisitorsError
        }
    }
}
