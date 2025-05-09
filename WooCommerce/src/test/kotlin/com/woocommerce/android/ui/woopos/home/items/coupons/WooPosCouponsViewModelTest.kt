package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemAddedToCart.WooPosItemSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosCouponsViewModelTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val listViewStateManager: WooPosCouponsListViewStateManager = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val couponCreationFacade: WooPosCouponCreationFacade = mock()
    private val navigator: WooPosItemsNavigator = mock()

    @Before
    fun setUp() {
        whenever(listViewStateManager.viewState).thenReturn(flowOf(WooPosCouponsViewState.Loading()))
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
