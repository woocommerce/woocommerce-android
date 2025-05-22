package com.woocommerce.android.ui.woopos.home.items

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.ui.coupons.CouponTestUtils
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchButtonTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosItemsViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val tabs = listOf(
        WooPosItemsViewState.Tab(
            R.string.woopos_products_screen_title,
            WooPosItemsViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsViewState.Tab(
            R.string.woopos_coupons_screen_title,
            WooPosItemsViewState.Tab.HighlightLevel.Normal
        )
    )

    private val wooPosItemsNavigator: WooPosItemsNavigator = mock()

    private val searchHelper: WooPosItemsSearchHelper = mock()
    private val tabsHelper: WooPosItemsTabsHelper = mock {
        on { defaultTabs }.thenReturn(tabs)
    }
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val couponCreationFacade: WooPosCouponCreationFacade = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()

    @Before
    fun setup() {
        whenever(searchHelper.getInitialSearchState()).thenReturn(
            WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )
    }

    @Test
    fun `given variations screen, when clicked back, then trigger proper event`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.BackButtonClicked)

        // THEN
        verify(wooPosItemsNavigator).sendNavigationEvent(
            WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
        )
    }

    @Test
    fun `when view model created, then search state is visible`() = runTest {
        // GIVEN
        whenever(searchHelper.getInitialSearchState()).thenReturn(
            WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsViewState.ProductList
            assertThat(contentState.search).isInstanceOf(WooPosItemsViewState.SearchState.Visible::class.java)
            val searchState = contentState.search as WooPosItemsViewState.SearchState.Visible
            assertThat(searchState.state).isEqualTo(WooPosSearchInputState.Closed)
        }
    }

    @Test
    fun `given search visible, when close search clicked, then search state is closed`() = runTest {
        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)

        // THEN
        verify(searchHelper).onCloseSearchClicked()
    }

    @Test
    fun `given search visible, when search text changed, then search helper is called`() = runTest {
        val query = "test query"
        val viewModel = createViewModel()

        viewModel.onUIEvent(WooPosItemsUIEvent.SearchChanged(query, 0))

        verify(searchHelper).onSearchChanged(query, 0)
    }

    @Test
    fun `given search visible, when clear search clicked, then search helper is called`() = runTest {
        val viewModel = createViewModel()

        viewModel.onUIEvent(WooPosItemsUIEvent.ClearSearchClicked)

        verify(searchHelper).onClearSearchClicked()
    }

    @Test
    fun `given search visible, when close search clicked, then search helper is called`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)

        verify(searchHelper).onCloseSearchClicked()
    }

    @Test
    fun `when tab clicked, then tab is selected and state is updated`() = runTest {
        // GIVEN
        val couponsTab = WooPosItemsViewState.Tab(
            R.string.woopos_coupons_screen_title,
            WooPosItemsViewState.Tab.HighlightLevel.Normal
        )

        whenever(tabsHelper.selectTab(any(), eq(couponsTab))).thenReturn(
            listOf(
                WooPosItemsViewState.Tab(
                    R.string.woopos_products_screen_title,
                    WooPosItemsViewState.Tab.HighlightLevel.Normal
                ),
                WooPosItemsViewState.Tab(
                    R.string.woopos_coupons_screen_title,
                    WooPosItemsViewState.Tab.HighlightLevel.Full
                )
            )
        )

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.OnTabClicked(couponsTab))

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosItemsViewState.CouponList::class.java)
        }
    }

    @Test
    fun `when search icon is tapped, then track analytics event`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.SearchIconClicked)

        // THEN
        verify(analyticsTracker).track(
            eq(
                SearchButtonTapped(
                    source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                )
            )
        )
    }

    @Test
    fun `when add coupon icon is tapped, then newly created coupon added to cart`() = runTest {
        // GIVEN
        whenever(couponCreationFacade.createCoupon())
            .thenReturn(CouponTestUtils.generateTestCoupon(1L, "test"))
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.AddCouponIconClicked)

        // THEN
        val item = ItemClickedData.Coupon(1L, "test")
        verify(fromChildToParentEventSender).sendToParent(
            eq(
                ChildToParentEvent.ItemClickedInProductSelector(
                    itemData = item,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = item,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                    )
                )
            )
        )
    }

    private fun createViewModel() =
        WooPosItemsViewModel(
            wooPosItemsNavigator,
            searchHelper,
            tabsHelper,
            couponCreationFacade,
            fromChildToParentEventSender,
            analyticsTracker,
        )
}
