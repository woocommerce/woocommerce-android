package com.woocommerce.android.ui.woopos.home.items.coupons

import app.cash.turbine.test
import com.woocommerce.android.ui.coupons.CouponTestUtils
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState.Content
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosGetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatCouponSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import com.woocommerce.android.model.Coupon as CouponDBModel

private const val MORE_PAGES_AVAILABLE = true
private const val MORE_PAGES_NOT_AVAILABLE = false

@ExperimentalCoroutinesApi
class WooPosCouponsListViewStateManagerTest {

    @JvmField
    @Rule
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val testViewModelScope = CoroutineScope(coroutinesTestRule.testDispatcher)

    private val formatCouponSummary: WooPosFormatCouponSummary = mock()
    private val getCachedStoreCurrency: WooPosGetCachedStoreCurrency = mock()
    private val couponsDataFlow = MutableStateFlow<List<CouponDBModel>>(emptyList())
    private val cachedCouponEnabledChecker: CachedCouponEnabledChecker = mock {
        onBlocking { isEnabled() } doReturn true
    }

    private val couponsDataSource: WooPosCouponsDataSource = mock {
        on { couponsFlow } doReturn couponsDataFlow
    }

    private val sat = WooPosCouponsListViewStateManager(
        couponsDataSource,
        formatCouponSummary,
        getCachedStoreCurrency,
        cachedCouponEnabledChecker
    )

    @Before
    fun setup() {
        wheneverBlocking { getCachedStoreCurrency() }.thenReturn("USD")
        whenever(formatCouponSummary.invoke(anyOrNull(), anyOrNull())).thenAnswer { "" }
    }

    @Test
    fun `given empty db, when fetching first page in progress, then Loading state`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(emptyList()) // cache empty
            delay(500)
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // remote data
            Result.success(true)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)

            // THEN
            assertThat(expectMostRecentItem()).isInstanceOf(WooPosCouponsViewState.Loading::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given full db, when fetching first page in progress, then cached data shown`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L))) // cache data
            delay(500)
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(2L))) // remote data
            Result.success(true)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)

            // THEN
            assertThat(expectMostRecentItem()).isInstanceOf(Content::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given cached data and fetching in progress, when content shown, then pullToRefreshState is Disabled`() =
        runTest {
            // GIVEN
            whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
                couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L)))
                delay(500)
                couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(2L))) // remote data
                Result.success(true)
            }

            sat.viewState.test {
                // WHEN
                sat.fetchCoupons(this, pullToRefresh = false)

                // THEN
                assertThat(expectMostRecentItem().pullToRefreshState)
                    .isEqualTo(WooPosPullToRefreshState.Disabled)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given empty db, when fetching first page completes, then Empty state`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L))) // remote
            delay(500)
            couponsDataFlow.emit(emptyList()) // remote
            Result.success(true)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)

            // THEN
            advanceUntilIdle()
            assertThat(expectMostRecentItem()).isInstanceOf(WooPosCouponsViewState.Empty::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given non-empty db, when fetching first page completes, then Content state`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            delay(500)
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L))) // remote
            Result.success(true)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)
            advanceUntilIdle()

            // THEN
            assertThat(expectMostRecentItem()).isInstanceOf(Content::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when first page fetch fails, then emits Loading then Error`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage())
            .doSuspendableAnswer {
                delay(1) // workaround for bug in mockito
                Result.failure(IllegalArgumentException("Test exception"))
            }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)

            // THEN
            skipItems(1) // Loading
            advanceUntilIdle()
            assertThat(awaitItem()).isInstanceOf(WooPosCouponsViewState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(WooPosCouponsViewState.Error::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given first page failed, when retry succeeds, then emits Content`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage())
            .doReturn(Result.failure<Boolean>(IllegalArgumentException("Test exception")))
        sat.fetchCoupons(testViewModelScope, pullToRefresh = false)
        advanceUntilIdle()
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            delay(1) // workaround for bug in mockito
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            Result.success(true)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)

            // THEN
            advanceUntilIdle()
            assertThat(expectMostRecentItem()).isInstanceOf(Content::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given first page failed, when retry fails, then emits Error`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage())
            .doReturn(Result.failure<Boolean>(IllegalArgumentException("Test exception")))
        sat.fetchCoupons(testViewModelScope, pullToRefresh = false)
        advanceUntilIdle()
        whenever(couponsDataSource.clearCacheAndFetchFirstPage())
            .doReturn(Result.failure<Boolean>(IllegalArgumentException("Test exception")))

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)

            // THEN
            advanceUntilIdle()
            assertThat(expectMostRecentItem()).isInstanceOf(WooPosCouponsViewState.Error::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given more pages available, when content shown, then pagination state loading`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            delay(1) // workaround for bug in mockito
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            Result.success(MORE_PAGES_AVAILABLE)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)

            advanceUntilIdle()

            // THEN
            val state = expectMostRecentItem() as Content
            assertThat(state.paginationState).isInstanceOf(WooPosPaginationState.Loading::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given more pages not available, when content shown, then pagination state None`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            delay(1) // workaround for bug in mockito
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            Result.success(MORE_PAGES_NOT_AVAILABLE)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)

            // THEN
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state).isInstanceOf(Content::class.java)
            assertThat((state as Content).paginationState)
                .isInstanceOf(WooPosPaginationState.None::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when end of list reached and load more fails, then pagination state Error`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_AVAILABLE)
        }
        sat.fetchCoupons(testViewModelScope, pullToRefresh = false)
        advanceUntilIdle()
        whenever(couponsDataSource.loadMore()).doSuspendableAnswer {
            delay(1) // workaround for bug in mockito
            Result.failure(IllegalArgumentException("Test exception"))
        }

        sat.viewState.test {
            // WHEN
            sat.endOfListReached(testViewModelScope)

            advanceUntilIdle()

            // THEN
            val state = expectMostRecentItem() as Content
            assertThat(state.paginationState).isInstanceOf(WooPosPaginationState.Error::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given pagination state error, when end of list reached, then nothing happens`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_AVAILABLE)
        }
        sat.fetchCoupons(testViewModelScope, pullToRefresh = false)
        advanceUntilIdle()
        whenever(couponsDataSource.loadMore()).doSuspendableAnswer {
            delay(1) // workaround for bug in mockito
            Result.failure(IllegalArgumentException("Test exception"))
        }
        sat.endOfListReached(this)

        sat.viewState.test {
            advanceUntilIdle()
            expectMostRecentItem() // ignore previous events
            // WHEN
            sat.endOfListReached(this)
            advanceUntilIdle()

            // THEN
            expectNoEvents() // no new events

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when load more retried, then pagination state Loading`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_AVAILABLE)
        }
        sat.fetchCoupons(testViewModelScope, pullToRefresh = false)
        advanceUntilIdle()
        whenever(couponsDataSource.loadMore()).doSuspendableAnswer {
            delay(1) // workaround for bug in mockito
            Result.failure(IllegalArgumentException("Test exception"))
        }

        sat.viewState.test {
            // WHEN
            sat.retryLoadMore(testViewModelScope)

            // THEN
            val state = expectMostRecentItem() as Content
            assertThat(state.paginationState).isInstanceOf(WooPosPaginationState.Loading::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cached data are still shown until remote request finishes`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L)))
            delay(500)
            couponsDataFlow.emit(emptyList()) // remote
            Result.success(true)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)
            skipItems(2) // Empty + cached data shown

            // THEN
            testScheduler.advanceTimeBy(499)
            expectNoEvents() // Still Cached data shown
            testScheduler.advanceTimeBy(2)

            assertThat(expectMostRecentItem()).isInstanceOf(WooPosCouponsViewState.Empty::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given coupons not enabled, when fetching coupons, then emits CouponsDisabledError`() = runTest {
        // GIVEN
        whenever(cachedCouponEnabledChecker.isEnabled()).doReturn(false)

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = false)

            // THEN
            assertThat(expectMostRecentItem())
                .isInstanceOf(WooPosCouponsViewState.Error.CouponsDisabledError::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when content shown and fetching first page, then pagination state loading`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            delay(500)
            Result.success(MORE_PAGES_AVAILABLE)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, false)

            // THEN
            val state = expectMostRecentItem() as Content
            assertThat(state.paginationState).isInstanceOf(WooPosPaginationState.Loading::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given state empty, when pull to refresh triggered, then loading shown with PTR`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(emptyList())
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_NOT_AVAILABLE)
        }
        sat.fetchCoupons(testViewModelScope, pullToRefresh = false)
        advanceUntilIdle()
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            delay(500)
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L)))
            Result.success(MORE_PAGES_NOT_AVAILABLE)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = true)

            // THEN
            val state = expectMostRecentItem()
            assertThat(state).isInstanceOf(WooPosCouponsViewState.Loading::class.java)
            assertThat((state as WooPosCouponsViewState.Loading).pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Refreshing)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given cached data shown, when pull to refresh triggered, then content shown with PTR`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L)))
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_NOT_AVAILABLE)
        }
        sat.fetchCoupons(testViewModelScope, pullToRefresh = false)
        advanceUntilIdle()
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            delay(500)
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L)))
            Result.success(MORE_PAGES_NOT_AVAILABLE)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, pullToRefresh = true)

            // THEN
            val state = expectMostRecentItem()
            assertThat(state).isInstanceOf(Content::class.java)
            assertThat((state as Content).pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Refreshing)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
