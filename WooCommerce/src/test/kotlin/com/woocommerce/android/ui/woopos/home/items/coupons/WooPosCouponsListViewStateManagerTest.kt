package com.woocommerce.android.ui.woopos.home.items.coupons

import app.cash.turbine.test
import com.woocommerce.android.ui.coupons.CouponTestUtils
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState.Content
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosGetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosCouponsFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Date
import com.woocommerce.android.model.Coupon as CouponDBModel

private const val MORE_PAGES_AVAILABLE = true
private const val MORE_PAGES_NOT_AVAILABLE = false

@ExperimentalCoroutinesApi
class WooPosCouponsListViewStateManagerTest {

    @JvmField
    @Rule
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val testViewModelScope = CoroutineScope(coroutinesTestRule.testDispatcher)

    private val couponFormatter: WooPosCouponsFormatter = mock()
    private val getCachedStoreCurrency: WooPosGetCachedStoreCurrency = mock()
    private val couponsDataFlow = MutableStateFlow<List<CouponDBModel>>(emptyList())
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val cachedCouponEnabledChecker: CachedCouponEnabledChecker = mock {
        on { isEnabled() } doReturn true
    }

    private val couponsDataSource: WooPosCouponsDataSource = mock {
        on { couponsFlow } doReturn couponsDataFlow
    }

    private val sat = WooPosCouponsListViewStateManager(
        couponsDataSource,
        couponFormatter,
        getCachedStoreCurrency,
        cachedCouponEnabledChecker,
        analyticsTracker,
    )

    @Before
    fun setup() {
        runBlocking { whenever(getCachedStoreCurrency()).thenReturn("USD") }
        whenever(couponFormatter.formatSummary(anyOrNull(), anyOrNull())).thenAnswer { "" }
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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)

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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)

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
                sat.fetchCoupons(
                    testViewModelScope,
                    WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL
                )

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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)

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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)

            // THEN
            skipItems(1) // Loading
            advanceUntilIdle()
            assertThat(awaitItem()).isInstanceOf(WooPosCouponsViewState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(WooPosCouponsViewState.Error::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given data in cache, when retry to fetch first page, then emits Loading`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage())
            .doSuspendableAnswer {
                couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
                delay(500)
                Result.failure(IllegalArgumentException("Test exception"))
            }

        sat.viewState.test {
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
            advanceUntilIdle()
            skipItems(3) // Empty + Loading + Error

            // WHEN
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.RETRY)

            // THEN
            advanceUntilIdle()
            assertThat(awaitItem()).isInstanceOf(WooPosCouponsViewState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(WooPosCouponsViewState.Error::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given data cache empty, when retry to fetch first page, then emits Loading`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage())
            .doSuspendableAnswer {
                delay(1) // workaround for bug in mockito
                Result.failure(IllegalArgumentException("Test exception"))
            }

        sat.viewState.test {
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
            advanceUntilIdle()
            skipItems(3) // Empty + Loading + Error

            // WHEN
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.RETRY)

            // THEN
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
        sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
        advanceUntilIdle()
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            delay(1) // workaround for bug in mockito
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            Result.success(true)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)

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
        sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
        advanceUntilIdle()
        whenever(couponsDataSource.clearCacheAndFetchFirstPage())
            .doReturn(Result.failure<Boolean>(IllegalArgumentException("Test exception")))

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.RETRY)

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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)

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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)

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
        sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
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
    fun `when end of list reached and load more fails, then PTR enabled`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_AVAILABLE)
        }
        sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
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
            assertThat(state.pullToRefreshState).isInstanceOf(WooPosPullToRefreshState.Enabled::class.java)

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
        sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
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
        sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)

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
            sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)

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
        sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
        advanceUntilIdle()
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            delay(500)
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L)))
            Result.success(MORE_PAGES_NOT_AVAILABLE)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(
                testViewModelScope,
                WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.PULL_TO_REFRESH
            )

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
        sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
        advanceUntilIdle()
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            delay(500)
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(1L)))
            Result.success(MORE_PAGES_NOT_AVAILABLE)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(
                testViewModelScope,
                WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.PULL_TO_REFRESH
            )

            // THEN
            val state = expectMostRecentItem()
            assertThat(state).isInstanceOf(Content::class.java)
            assertThat((state as Content).pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Refreshing)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given coupon without expiration date, when mapped, then expiredState is NotExpired`() = runTest {
        // GIVEN
        val coupon = CouponTestUtils.generateTestCoupon(0L).copy(
            dateExpiresGmt = null
        )
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(coupon))
            delay(1) // workaround for bug in mockito
            Result.success(false)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(
                testViewModelScope,
                WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL
            )
            advanceUntilIdle()

            // THEN
            val state = expectMostRecentItem() as Content
            val mappedCoupon = state.items.first() as WooPosItemSelectionViewState.Coupon
            assertThat(mappedCoupon.expiredState)
                .isInstanceOf(WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given coupon with future expiration date, when mapped, then expiredState is NotExpired`() = runTest {
        // GIVEN
        val futureDate = Date(System.currentTimeMillis() + 86400000) // tomorrow
        val coupon = CouponTestUtils.generateTestCoupon(0L).copy(
            dateExpiresGmt = futureDate
        )
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(coupon))
            delay(1) // workaround for bug in mockito
            Result.success(false)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(
                testViewModelScope,
                WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL
            )
            advanceUntilIdle()

            // THEN
            val state = expectMostRecentItem() as Content
            val mappedCoupon = state.items.first() as WooPosItemSelectionViewState.Coupon
            assertThat(mappedCoupon.expiredState)
                .isInstanceOf(WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given coupon with past expiration date, when mapped, then expiredState is Expired with formatted date`() =
        runTest {
            // GIVEN
            val pastDate = Date(System.currentTimeMillis() - 86400000) // yesterday
            val localExpiryDate = LocalDate.of(2099, 1, 1)
            val coupon = CouponTestUtils.generateTestCoupon(0L).copy(
                dateExpiresGmt = pastDate,
                dateExpiresLocal = localExpiryDate
            )
            val formattedDate = "01 Jan 2023"
            whenever(couponFormatter.formatExpiredText(localExpiryDate)).thenReturn(formattedDate)
            whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
                couponsDataFlow.emit(listOf(coupon))
                delay(1) // workaround for bug in mockito
                Result.success(false)
            }

            sat.viewState.test {
                // WHEN
                sat.fetchCoupons(
                    testViewModelScope,
                    WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL
                )
                advanceUntilIdle()

                // THEN
                val state = expectMostRecentItem() as Content
                val mappedCoupon = state.items.first() as WooPosItemSelectionViewState.Coupon
                assertThat(mappedCoupon.expiredState)
                    .isInstanceOf(WooPosItemSelectionViewState.Coupon.ExpiredState.Expired::class.java)
                val expiredState = mappedCoupon.expiredState as WooPosItemSelectionViewState.Coupon.ExpiredState.Expired
                assertThat(expiredState.formattedDate).isEqualTo(formattedDate)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given expired coupon without local expiry, when mapped, then expired state has no formatted date`() = runTest {
        // GIVEN
        val coupon = CouponTestUtils.generateTestCoupon(0L).copy(
            dateExpiresGmt = Date(System.currentTimeMillis() - 86400000),
            dateExpiresLocal = null
        )
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(coupon))
            delay(1) // workaround for bug in mockito
            Result.success(false)
        }

        sat.viewState.test {
            // WHEN
            sat.fetchCoupons(
                testViewModelScope,
                WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL
            )
            advanceUntilIdle()

            // THEN
            val state = expectMostRecentItem() as Content
            val mappedCoupon = state.items.first() as WooPosItemSelectionViewState.Coupon
            val expiredState = mappedCoupon.expiredState as WooPosItemSelectionViewState.Coupon.ExpiredState.Expired
            assertThat(expiredState.formattedDate).isNull()
            verify(couponFormatter, never()).formatExpiredText(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when next page loads without search query, then tracks event with LIST source type`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage()).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_AVAILABLE)
        }
        sat.fetchCoupons(testViewModelScope, WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.INITIAL)
        advanceUntilIdle()
        whenever(couponsDataSource.loadMore()).doSuspendableAnswer {
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_AVAILABLE)
        }

        sat.viewState.test {
            // WHEN
            sat.endOfListReached(testViewModelScope)

            advanceUntilIdle()

            // THEN
            verify(analyticsTracker).track(
                eq(
                    WooPosAnalyticsEvent.Event.ItemsNextPageLoaded(
                        source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                    )
                )
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when next page loads with search query, then tracks event with SEARCH_RESULT source type`() = runTest {
        // GIVEN
        whenever(couponsDataSource.clearCacheAndFetchFirstPage(any())).doSuspendableAnswer {
            couponsDataFlow.emit(listOf(CouponTestUtils.generateTestCoupon(0L))) // cache
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_AVAILABLE)
        }
        whenever(couponsDataSource.loadMore()).doSuspendableAnswer {
            delay(1) // workaround for bug in mockito
            Result.success(MORE_PAGES_AVAILABLE)
        }

        sat.setSearchQuery("test", testViewModelScope)
        advanceUntilIdle()

        sat.viewState.test {
            // WHEN
            sat.endOfListReached(testViewModelScope)

            advanceUntilIdle()

            // THEN
            verify(analyticsTracker).track(
                eq(
                    WooPosAnalyticsEvent.Event.ItemsNextPageLoaded(
                        source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT
                    )
                )
            )

            cancelAndIgnoreRemainingEvents()
        }
    }
}
