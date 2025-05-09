package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.BackButtonClicked
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.CouponClicked
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.EndOfListReached
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.PullToRefreshTriggered
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.RetryLoadMoreTriggered
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.RetryTriggered
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemAddedToCart.WooPosItemSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosCouponsViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val listViewStateManager: WooPosCouponsListViewStateManager = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val navigator: WooPosItemsNavigator = mock()

    private lateinit var viewModel: WooPosCouponsViewModel

    @Before
    fun setUp() {
        whenever(listViewStateManager.viewState).thenReturn(flowOf(WooPosCouponsViewState.Loading()))
        viewModel = createViewModel()
    }

    @Test
    fun `when coupon clicked, then send item clicked event`() = runTest {
        // Given
        val couponId = 123L

        // When
        viewModel.onUIEvent(CouponClicked(couponId))

        // Then
        verify(fromChildToParentEventSender).sendToParent(
            ChildToParentEvent.ItemClickedInProductSelector(
                itemData = ItemClickedData.Coupon(couponId),
                source = WooPosItemSource.COUPON_LIST
            )
        )
    }

    @Test
    fun `when pull to refresh triggered, then fetch coupons with pull to refresh type`() {
        // When
        viewModel.onUIEvent(PullToRefreshTriggered)

        // Then
        verify(listViewStateManager).fetchCoupons(
            viewModelScope = any(),
            refreshType = eq(WooPosCouponsListRefreshType.PULL_TO_REFRESH)
        )
    }

    @Test
    fun `when end of list reached, then call end of list reached on state manager`() {
        // When
        viewModel.onUIEvent(EndOfListReached)

        // Then
        verify(listViewStateManager).endOfListReached(any())
    }

    @Test
    fun `when retry load more triggered, then call retry load more on state manager`() {
        // When
        viewModel.onUIEvent(RetryLoadMoreTriggered)

        // Then
        verify(listViewStateManager).retryLoadMore(any())
    }

    @Test
    fun `when back button clicked, then navigate back to item list screen`() = runTest {
        // When
        viewModel.onUIEvent(BackButtonClicked)

        // Then
        verify(navigator).sendNavigationEvent(
            WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
        )
    }

    @Test
    fun `when retry triggered, then fetch coupons with retry type`() {
        // When
        viewModel.onUIEvent(RetryTriggered)

        // Then
        verify(listViewStateManager).fetchCoupons(
            viewModelScope = any(),
            refreshType = eq(WooPosCouponsListRefreshType.RETRY)
        )
    }

    @Test
    fun `when init, then fetch coupons with initial type`() {
        // Then (init happens in createViewModel)
        verify(listViewStateManager).fetchCoupons(
            viewModelScope = any(),
            refreshType = eq(WooPosCouponsListRefreshType.INITIAL)
        )
    }

    private fun createViewModel() =
        WooPosCouponsViewModel(
            listViewStateManager,
            fromChildToParentEventSender,
            navigator,
        )
}
