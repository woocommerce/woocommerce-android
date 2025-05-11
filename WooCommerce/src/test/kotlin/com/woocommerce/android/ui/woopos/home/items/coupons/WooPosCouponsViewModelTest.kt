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
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
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

    private val couponCreationFacade: WooPosCouponCreationFacade = mock()
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
        // GIVEN
        val couponId = 123L

        // WHEN
        viewModel.onUIEvent(CouponClicked(couponId))

        // THEN
        verify(fromChildToParentEventSender).sendToParent(
            ChildToParentEvent.ItemClickedInProductSelector(
                itemData = ItemClickedData.Coupon(couponId),
                source = WooPosItemSource.COUPON_LIST
            )
        )
    }

    @Test
    fun `when pull to refresh triggered, then fetch coupons with pull to refresh type`() {
        // WHEN
        viewModel.onUIEvent(PullToRefreshTriggered)

        // THEN
        verify(listViewStateManager).fetchCoupons(
            viewModelScope = any(),
            refreshType = eq(WooPosCouponsListRefreshType.PULL_TO_REFRESH)
        )
    }

    @Test
    fun `when end of list reached, then call end of list reached on state manager`() {
        // WHEN
        viewModel.onUIEvent(EndOfListReached)

        // THEN
        verify(listViewStateManager).endOfListReached(any())
    }

    @Test
    fun `when retry load more triggered, then call retry load more on state manager`() {
        // WHEN
        viewModel.onUIEvent(RetryLoadMoreTriggered)

        // THEN
        verify(listViewStateManager).retryLoadMore(any())
    }

    @Test
    fun `when back button clicked, then navigate back to item list screen`() = runTest {
        // WHEN
        viewModel.onUIEvent(BackButtonClicked)

        // THEN
        verify(navigator).sendNavigationEvent(
            WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
        )
    }

    @Test
    fun `when retry triggered, then fetch coupons with retry type`() {
        // WHEN
        viewModel.onUIEvent(RetryTriggered)

        // THEN
        verify(listViewStateManager).fetchCoupons(
            viewModelScope = any(),
            refreshType = eq(WooPosCouponsListRefreshType.RETRY)
        )
    }

    @Test
    fun `when init, then fetch coupons with initial type`() {
        // THEN
        verify(listViewStateManager).fetchCoupons(
            viewModelScope = any(),
            refreshType = eq(WooPosCouponsListRefreshType.INITIAL)
        )
    }

    @Test
    fun `when add coupon icon is tapped, then newly created coupon added to cart`() = runTest {
        // GIVEN
        whenever(couponCreationFacade.createCoupon()).thenReturn(1L)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosCouponsUIEvent.CreateCouponClicked)

        // THEN
        verify(fromChildToParentEventSender).sendToParent(
            ChildToParentEvent.ItemClickedInProductSelector(
                itemData = ItemClickedData.Coupon(1L),
                source = WooPosItemSource.COUPON_LIST
            )
        )
    }

    private fun createViewModel() =
        WooPosCouponsViewModel(
            listViewStateManager,
            fromChildToParentEventSender,
            couponCreationFacade,
            navigator,
        )
}
