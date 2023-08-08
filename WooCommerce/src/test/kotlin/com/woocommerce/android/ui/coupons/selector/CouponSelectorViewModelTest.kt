package com.woocommerce.android.ui.coupons.selector

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.coupons.CouponListHandler
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class CouponSelectorViewModelTest : BaseUnitTest() {
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val couponsStateFlow = MutableStateFlow(emptyList<Coupon>())
    private val couponListHandler: CouponListHandler = mock {
        on { couponsFlow } doReturn couponsStateFlow
    }
    private val currencyFormatter: CurrencyFormatter = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val couponUtils = CouponUtils(
        currencyFormatter = currencyFormatter,
        resourceProvider = resourceProvider
    )
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: CouponSelectorViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = CouponSelectorViewModel(
            couponListHandler = couponListHandler,
            wooCommerceStore = wooCommerceStore,
            selectedSite = selectedSite,
            couponUtils = couponUtils,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            savedState = SavedStateHandle()
        )
    }

    @Test
    fun `when screen is loaded, then fetch coupons`() = testBlocking {
        // WHEN
        setup {
            whenever(couponListHandler.fetchCoupons(forceRefresh = true)).doSuspendableAnswer {
                delay(1L)
                return@doSuspendableAnswer Result.success(Unit)
            }
        }

        val fetchingState = viewModel.couponSelectorState.captureValues().last()

        // THEN

        verify(couponListHandler).fetchCoupons(forceRefresh = true)
        assertThat(fetchingState.loadingState).isEqualTo(LoadingState.Loading)
    }

    @Test
    fun `when load more is requested, then load next page`() = testBlocking {
        setup()

        // WHEN
        viewModel.onLoadMore()

        // THEN

        verify(couponListHandler).loadMore()
    }

    @Test
    fun `when list is swiped down, then force refresh coupon lists`() = testBlocking {
        setup()
        clearInvocations(couponListHandler)

        // WHEN
        viewModel.onRefresh()

        // THEN
        verify(couponListHandler).fetchCoupons(forceRefresh = true)
    }

    @Test
    fun `when fetching coupons fails, then show an error`() = testBlocking {
        // WHEN
        setup {
            whenever(couponListHandler.fetchCoupons(forceRefresh = true)).thenReturn(Result.failure(Exception()))
        }

        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        // THEN
        assertThat(event.message).isEqualTo(R.string.coupon_list_loading_failed)
    }

    @Test
    fun `when loading next page fails, then show an error`() = testBlocking {
        // WHEN
        setup {
            whenever(couponListHandler.loadMore()).thenReturn(Result.failure(Exception()))
        }
        viewModel.onLoadMore()
        advanceUntilIdle()
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        // THEN
        assertThat(event.message).isEqualTo(R.string.coupon_list_loading_failed)
    }

    @Test
    fun `when refreshing fails, then show an error`() = testBlocking {
        // WHEN
        setup {
            whenever(couponListHandler.fetchCoupons(forceRefresh = true)).thenReturn(Result.failure(Exception()))
        }

        viewModel.onRefresh()
        advanceUntilIdle()
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        // THEN
        assertThat(event.message).isEqualTo(R.string.coupon_list_loading_failed)
    }

    @Test
    fun `given coupon list empty, when user click on Go to Coupons List, then event is tracked`() = testBlocking {
        // GIVEN
        setup {
            whenever(couponListHandler.couponsFlow).thenReturn(couponsStateFlow)
        }

        // WHEN
        viewModel.onEmptyScreenButtonClicked()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_GO_TO_COUPON_BUTTON_TAPPED)
    }
}
