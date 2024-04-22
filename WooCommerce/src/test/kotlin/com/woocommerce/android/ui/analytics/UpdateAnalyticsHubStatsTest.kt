package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.BundleStat
import com.woocommerce.android.model.GiftCardsStat
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.BundlesResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.FetchStrategy.ForceNew
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.FetchStrategy.Saved
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.GiftCardResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.OrdersResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.ProductsResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.RevenueResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.VisitorsResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.hub.sync.BundlesState
import com.woocommerce.android.ui.analytics.hub.sync.GiftCardsState
import com.woocommerce.android.ui.analytics.hub.sync.OrdersState
import com.woocommerce.android.ui.analytics.hub.sync.ProductsState
import com.woocommerce.android.ui.analytics.hub.sync.RevenueState
import com.woocommerce.android.ui.analytics.hub.sync.SessionState
import com.woocommerce.android.ui.analytics.hub.sync.UpdateAnalyticsHubStats
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
internal class UpdateAnalyticsHubStatsTest : BaseUnitTest() {
    private lateinit var analyticsDataStore: AnalyticsUpdateDataStore
    private lateinit var repository: AnalyticsRepository

    private lateinit var sut: UpdateAnalyticsHubStats

    @Before
    fun setUp() {
        analyticsDataStore = mock {
            onBlocking { shouldUpdateAnalytics(testRangeSelection) } doReturn flowOf(true)
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
    fun `when syncing bundles stats data successfully, then update the bundles with the expected states`() =
        testBlocking {
            // Given
            configureSuccessResponseStub()
            val bundleStatsUpdates = mutableListOf<BundlesState>()
            val job = sut.bundlesState
                .onEach { bundleStatsUpdates.add(it) }
                .launchIn(this)

            // When
            sut(testRangeSelection, this)

            advanceUntilIdle()

            // Then
            assertThat(bundleStatsUpdates).hasSize(3)
            assertThat(bundleStatsUpdates[0]).isEqualTo(BundlesState.Available(BundleStat.EMPTY))
            assertThat(bundleStatsUpdates[1]).isEqualTo(BundlesState.Loading)
            assertThat(bundleStatsUpdates[2]).isEqualTo(BundlesState.Available(testBundleStat))

            job.cancel()
        }

    @Test
    fun `when syncing bundles stats data fails, then update the bundles with the expected states`() = testBlocking {
        // Given
        configureErrorResponseStub()
        val bundleStatsUpdates = mutableListOf<BundlesState>()
        val job = sut.bundlesState
            .onEach { bundleStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(bundleStatsUpdates).hasSize(3)
        assertThat(bundleStatsUpdates[0]).isEqualTo(BundlesState.Available(BundleStat.EMPTY))
        assertThat(bundleStatsUpdates[1]).isEqualTo(BundlesState.Loading)
        assertThat(bundleStatsUpdates[2]).isEqualTo(BundlesState.Error)

        job.cancel()
    }

    @Test
    fun `when syncing gift card stats data successfully, then update the bundles with the expected states`() =
        testBlocking {
            // Given
            configureSuccessResponseStub()
            val giftCardStatsUpdates = mutableListOf<GiftCardsState>()
            val job = sut.giftCardsState
                .onEach { giftCardStatsUpdates.add(it) }
                .launchIn(this)

            // When
            sut(testRangeSelection, this)

            advanceUntilIdle()

            // Then
            assertThat(giftCardStatsUpdates).hasSize(3)
            assertThat(giftCardStatsUpdates[0]).isEqualTo(GiftCardsState.Available(GiftCardsStat.EMPTY))
            assertThat(giftCardStatsUpdates[1]).isEqualTo(GiftCardsState.Loading)
            assertThat(giftCardStatsUpdates[2]).isEqualTo(GiftCardsState.Available(testGiftCardStat))

            job.cancel()
        }

    @Test
    fun `when syncing gift cards stats data fails, then update the bundles with the expected states`() = testBlocking {
        // Given
        configureErrorResponseStub()
        val giftCardStatsUpdates = mutableListOf<GiftCardsState>()
        val job = sut.giftCardsState
            .onEach { giftCardStatsUpdates.add(it) }
            .launchIn(this)

        // When
        sut(testRangeSelection, this)

        advanceUntilIdle()

        // Then
        assertThat(giftCardStatsUpdates).hasSize(3)
        assertThat(giftCardStatsUpdates[0]).isEqualTo(GiftCardsState.Available(GiftCardsStat.EMPTY))
        assertThat(giftCardStatsUpdates[1]).isEqualTo(GiftCardsState.Loading)
        assertThat(giftCardStatsUpdates[2]).isEqualTo(GiftCardsState.Error)

        job.cancel()
    }

    @Test
    fun `when syncing stats with an unsupported visitors granularity, then update the session with the unsupported states`() =
        testBlocking {
            // Given
            configureSuccessResponseStub()
            whenever(
                repository.fetchVisitorsData(
                    testRangeSelection,
                    ForceNew
                )
            ).doReturn(VisitorsResult.VisitorsNotSupported)

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
            assertThat(sessionStatsUpdates[2]).isEqualTo(SessionState.NotSupported)

            job.cancel()
        }

    @Test
    fun `when data store allows new stats fetch, then request data with ForceNew strategy`() = testBlocking {
        // When
        sut(testRangeSelection, this)

        // Then
        verify(repository).fetchRevenueData(testRangeSelection, ForceNew)
        verify(repository).fetchOrdersData(testRangeSelection, ForceNew)
        verify(repository).fetchVisitorsData(testRangeSelection, ForceNew)
        verify(repository).fetchProductsData(testRangeSelection, ForceNew)
    }

    @Test
    fun `when data store does NOT allows net stats fetch, then request data with Saved strategy`() = testBlocking {
        // Given
        analyticsDataStore = mock {
            onBlocking { shouldUpdateAnalytics(testRangeSelection) } doReturn flowOf(false)
        }
        sut = UpdateAnalyticsHubStats(
            analyticsUpdateDataStore = analyticsDataStore,
            analyticsRepository = repository
        )

        // When
        sut(testRangeSelection, this)

        // Then
        verify(repository).fetchRevenueData(testRangeSelection, Saved)
        verify(repository).fetchOrdersData(testRangeSelection, Saved)
        verify(repository).fetchVisitorsData(testRangeSelection, Saved)
        verify(repository).fetchProductsData(testRangeSelection, Saved)
    }

    @Test
    fun `when selection type is CUSTOM, then follow the data store and request data with Stored strategy`() =
        testBlocking {
            // Given
            analyticsDataStore = mock {
                onBlocking { shouldUpdateAnalytics(testCustomRangeSelection) } doReturn flowOf(false)
            }
            sut = UpdateAnalyticsHubStats(
                analyticsUpdateDataStore = analyticsDataStore,
                analyticsRepository = repository
            )

            // When
            sut(testCustomRangeSelection, this)

            // Then
            verify(repository).fetchRevenueData(testCustomRangeSelection, Saved)
            verify(repository).fetchOrdersData(testCustomRangeSelection, Saved)
            verify(repository).fetchVisitorsData(testCustomRangeSelection, Saved)
            verify(repository).fetchProductsData(testCustomRangeSelection, Saved)
        }

    @Test
    fun `when syncing stats data starts with forceUpdate true, then trigger update with ForceNew Strategy`() =
        testBlocking {
            // Given
            analyticsDataStore = mock {
                onBlocking { shouldUpdateAnalytics(testRangeSelection) } doReturn flowOf(false)
            }
            sut = UpdateAnalyticsHubStats(
                analyticsUpdateDataStore = analyticsDataStore,
                analyticsRepository = repository
            )

            // When
            sut(testRangeSelection, this, true)

            // Then
            verify(repository).fetchRevenueData(testRangeSelection, ForceNew)
            verify(repository).fetchOrdersData(testRangeSelection, ForceNew)
            verify(repository).fetchVisitorsData(testRangeSelection, ForceNew)
            verify(repository).fetchProductsData(testRangeSelection, ForceNew)

            verify(analyticsDataStore, never()).shouldUpdateAnalytics(testRangeSelection)
        }

    @Test
    fun `when syncing stats data starts with forceUpdate false, then follow data store response`() = testBlocking {
        // Given
        analyticsDataStore = mock {
            onBlocking { shouldUpdateAnalytics(testRangeSelection) } doReturn flowOf(false)
        }
        sut = UpdateAnalyticsHubStats(
            analyticsUpdateDataStore = analyticsDataStore,
            analyticsRepository = repository
        )

        // When
        sut(testRangeSelection, this, false)

        // Then
        verify(repository).fetchRevenueData(testRangeSelection, Saved)
        verify(repository).fetchOrdersData(testRangeSelection, Saved)
        verify(repository).fetchVisitorsData(testRangeSelection, Saved)
        verify(repository).fetchProductsData(testRangeSelection, Saved)

        verify(analyticsDataStore).shouldUpdateAnalytics(testRangeSelection)
    }

    @Test
    fun `when syncing stats data starts with ForceNew strategy, then store the expected timestamp`() = testBlocking {
        // Given
        configureSuccessResponseStub()

        // When
        sut(testRangeSelection, this)

        // Then
        verify(analyticsDataStore, atLeast(1)).storeLastAnalyticsUpdate(eq(testRangeSelection), any())
    }

    @Test
    fun `when syncing stats data stats with Stored strategy, then do not store the timestamp`() = testBlocking {
        // Given
        configureSuccessResponseStub()
        analyticsDataStore = mock {
            onBlocking { shouldUpdateAnalytics(testRangeSelection) } doReturn flowOf(false)
        }
        sut = UpdateAnalyticsHubStats(
            analyticsUpdateDataStore = analyticsDataStore,
            analyticsRepository = repository
        )

        // When
        sut(testRangeSelection, this)

        // Then
        verify(analyticsDataStore, never()).storeLastAnalyticsUpdate(testRangeSelection)
    }

    @Test
    fun `when syncing stats with empty DataStore flow response, then use ForceNew strategy`() = testBlocking {
        // Given
        configureSuccessResponseStub()
        analyticsDataStore = mock {
            onBlocking { shouldUpdateAnalytics(testRangeSelection) } doReturn emptyFlow()
        }
        sut = UpdateAnalyticsHubStats(
            analyticsUpdateDataStore = analyticsDataStore,
            analyticsRepository = repository
        )

        // When
        sut(testRangeSelection, this)

        // Then
        verify(repository).fetchRevenueData(testRangeSelection, ForceNew)
        verify(repository).fetchOrdersData(testRangeSelection, ForceNew)
        verify(repository).fetchVisitorsData(testRangeSelection, ForceNew)
        verify(repository).fetchProductsData(testRangeSelection, ForceNew)
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
            onBlocking {
                repository.fetchProductBundlesStats(testRangeSelection)
            } doReturn testBundlesResult
            onBlocking {
                repository.fetchGiftCardsStats(testRangeSelection)
            } doReturn testGiftCardResult
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
            onBlocking {
                repository.fetchProductBundlesStats(testRangeSelection)
            } doReturn BundlesResult.BundlesError
            onBlocking {
                repository.fetchGiftCardsStats(testRangeSelection)
            } doReturn GiftCardResult.GiftCardError
        }
    }
}
