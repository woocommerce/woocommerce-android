package com.woocommerce.android.ui.coupons

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

class CouponsListViewModelTests : BaseUnitTest() {
    private lateinit var viewModel: CouponListViewModel

    private val defaultCouponsList = (1L..10L).map {
        CouponTestUtils.generateTestCoupon(it)
    }
    private val couponsStateFlow = MutableStateFlow(emptyList<Coupon>())

    private val couponListHandler: CouponListHandler = mock {
        on { couponsFlow } doReturn couponsStateFlow
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
        on { getString(any(), anyVararg()) } doAnswer { it.arguments[0].toString() }
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val couponUtils = CouponUtils(
        currencyFormatter = currencyFormatter,
        resourceProvider = resourceProvider
    )

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = CouponListViewModel(
            savedState = SavedStateHandle(),
            wooCommerceStore = wooCommerceStore,
            couponListHandler = couponListHandler,
            couponUtils = couponUtils,
            selectedSite = mock {
                on { get() } doReturn SiteModel()
            },
            analyticsTrackerWrapper = analyticsTrackerWrapper,
        )
    }

    @Test
    fun `when screen is loaded, then fetch coupons`() = testBlocking {
        setup {
            whenever(couponListHandler.fetchCoupons(null, true)).doSuspendableAnswer {
                delay(1L)
                return@doSuspendableAnswer Result.success(Unit)
            }
        }

        val fetchingState = viewModel.couponsState.captureValues().last()

        verify(couponListHandler).fetchCoupons(searchQuery = null, forceRefresh = true)
        assertThat(fetchingState.loadingState).isEqualTo(CouponListViewModel.LoadingState.Loading)

        advanceUntilIdle()
        val idleState = viewModel.couponsState.captureValues().last()
        assertThat(idleState.loadingState).isEqualTo(CouponListViewModel.LoadingState.Idle)
    }

    @Test
    fun `when screen is loaded, then load saved coupons`() = testBlocking {
        setup()

        val state = viewModel.couponsState.runAndCaptureValues {
            couponsStateFlow.value = defaultCouponsList
            advanceUntilIdle()
        }.last()

        assertThat(state.coupons.map { it.id }).isEqualTo(defaultCouponsList.map { it.id })
        assertThat(state.loadingState).isEqualTo(CouponListViewModel.LoadingState.Idle)
    }

    @Test
    fun `when search is opened, then update state`() = testBlocking {
        setup()

        viewModel.onSearchStateChanged(true)

        val state = viewModel.couponsState.captureValues().last()
        assertThat(state.isSearchOpen).isTrue()
    }

    @Test
    fun `when search is opened, then proper track event is triggered`() = testBlocking {
        setup()

        viewModel.onSearchStateChanged(true)

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.COUPONS_LIST_SEARCH_TAPPED)
    }

    @Test
    fun `when search is NOT opened, then track event is NOT triggered`() = testBlocking {
        setup()

        viewModel.onSearchStateChanged(false)

        verify(analyticsTrackerWrapper, never()).track(AnalyticsEvent.COUPONS_LIST_SEARCH_TAPPED)
    }

    @Test
    fun `when search query is entered, then search for coupons`() = testBlocking {
        setup()

        val state = viewModel.couponsState.runAndCaptureValues {
            viewModel.onSearchQueryChanged("Search")
            advanceUntilIdle()
        }.last()

        assertThat(state.searchQuery).isEqualTo("Search")
        verify(couponListHandler).fetchCoupons("Search", forceRefresh = false)
    }

    @Test
    fun `when load more is requested, then load next page`() = testBlocking {
        setup()

        viewModel.onLoadMore()

        verify(couponListHandler).loadMore()
    }

    @Test
    fun `when list is swiped, then force refresh the coupons`() = testBlocking {
        setup()
        clearInvocations(couponListHandler)

        viewModel.onRefresh()

        verify(couponListHandler).fetchCoupons(forceRefresh = true)
    }

    @Test
    fun `when fetching coupons fails, then show an error`() = testBlocking {
        setup {
            whenever(couponListHandler.fetchCoupons(forceRefresh = true)).thenReturn(Result.failure(Exception()))
        }

        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        assertThat(event.message).isEqualTo(R.string.coupon_list_loading_failed)
    }

    @Test
    fun `when searching coupons fails, then show an error`() = testBlocking {
        setup {
            whenever(couponListHandler.fetchCoupons(any(), anyBoolean())).thenReturn(Result.failure(Exception()))
        }

        viewModel.onSearchQueryChanged("Search")
        advanceUntilIdle()
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        assertThat(event.message).isEqualTo(R.string.coupon_list_search_failed)
    }

    @Test
    fun `when loading next page, then show an error`() = testBlocking {
        setup {
            whenever(couponListHandler.loadMore()).thenReturn(Result.failure(Exception()))
        }

        viewModel.onLoadMore()
        advanceUntilIdle()
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        assertThat(event.message).isEqualTo(R.string.coupon_list_loading_failed)
    }

    @Test
    fun `when refreshing fails, then show an error`() = testBlocking {
        setup {
            whenever(couponListHandler.fetchCoupons(forceRefresh = true)).thenReturn(Result.failure(Exception()))
        }

        viewModel.onRefresh()
        advanceUntilIdle()
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        assertThat(event.message).isEqualTo(R.string.coupon_list_loading_failed)
    }
}
