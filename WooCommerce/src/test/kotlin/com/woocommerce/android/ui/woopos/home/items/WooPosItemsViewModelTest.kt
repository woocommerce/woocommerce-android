package com.woocommerce.android.ui.woopos.home.items

import app.cash.turbine.test
import com.woocommerce.android.ui.coupons.CouponTestUtils
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.localcatalog.DateTimeProvider
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncRequirement
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncStatusChecker
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsWooCommerceVersionSunsetWarningRequired
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchButtonTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
        WooPosItemsToolbarViewState.Tab.Products(
            "Products",
            WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsToolbarViewState.Tab.Coupons(
            "Coupons",
            WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        )
    )

    private val searchHelper: WooPosItemsSearchHelper = mock()
    private val tabsHelper: WooPosItemsTabsHelper = mock {
        on { defaultTabs }.thenReturn(tabs)
    }
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val couponCreationFacade: WooPosCouponCreationFacade = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val preferencesRepository: WooPosPreferencesRepository = mock()
    private val syncStatusChecker: WooPosFullSyncStatusChecker = mock()
    private val dateTimeProvider: DateTimeProvider = mock()
    private val isWooVersionSunsetWarningRequired: WooPosIsWooCommerceVersionSunsetWarningRequired = mock()
    private val resourceProvider: com.woocommerce.android.viewmodel.ResourceProvider = mock {
        on { getString(any()) }.thenReturn("Custom amount")
    }

    @Before
    fun setup() = runTest {
        whenever(searchHelper.getInitialSearchState()).thenReturn(
            WooPosItemsToolbarViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )
        whenever(parentToChildrenEventReceiver.events).thenReturn(flowOf())
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NotRequired(System.currentTimeMillis())
        )
        whenever(dateTimeProvider.now()).thenReturn(1000)
        whenever(isWooVersionSunsetWarningRequired()).thenReturn(false)
    }

    @Test
    fun `when view model created, then search state is visible`() = runTest {
        // GIVEN
        whenever(searchHelper.getInitialSearchState()).thenReturn(
            WooPosItemsToolbarViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsToolbarViewState.ProductList
            assertThat(contentState.search).isInstanceOf(WooPosItemsToolbarViewState.SearchState.Visible::class.java)
            val searchState = contentState.search as WooPosItemsToolbarViewState.SearchState.Visible
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
        // GIVEN
        val query = "test query"
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.SearchChanged(query, 0))

        // THEN
        verify(searchHelper).onSearchChanged(query, 0)
    }

    @Test
    fun `given search visible, when clear search clicked, then search helper is called`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.ClearSearchClicked)

        // THEN
        verify(searchHelper).onClearSearchClicked()
    }

    @Test
    fun `given search visible, when close search clicked, then search helper is called`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)

        // THEN
        verify(searchHelper).onCloseSearchClicked()
    }

    @Test
    fun `when tab clicked, then tab is selected and state is updated`() = runTest {
        // GIVEN
        val couponsTab = WooPosItemsToolbarViewState.Tab.Coupons(
            "Coupons",
            WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        )

        whenever(tabsHelper.selectTab(any(), eq(couponsTab))).thenReturn(
            listOf(
                WooPosItemsToolbarViewState.Tab.Products(
                    "Products",
                    WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
                ),
                WooPosItemsToolbarViewState.Tab.Coupons(
                    "Coupons",
                    WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
                )
            )
        )

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.OnTabClicked(couponsTab))

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosItemsToolbarViewState.CouponList::class.java)
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
    fun `when search icon is tapped on coupon tab, then track analytics event with COUPON source`() = runTest {
        // GIVEN
        val couponsTab = WooPosItemsToolbarViewState.Tab.Coupons(
            "Coupons",
            WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        )

        whenever(tabsHelper.selectTab(any(), eq(couponsTab))).thenReturn(
            listOf(
                WooPosItemsToolbarViewState.Tab.Products(
                    "Products",
                    WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
                ),
                WooPosItemsToolbarViewState.Tab.Coupons(
                    "Coupons",
                    WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
                )
            )
        )

        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.OnTabClicked(couponsTab))

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.SearchIconClicked)

        // THEN
        verify(analyticsTracker).track(
            eq(
                SearchButtonTapped(
                    source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                )
            )
        )
    }

    @Test
    fun `when search icon is tapped, then search helper is called with empty query`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.SearchIconClicked)

        // THEN
        verify(searchHelper).onSearchChanged("", 0)
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
                ChildToParentEvent.ItemClickedInItemsList(
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

    @Test
    fun `when add coupon icon is tapped, then track analytics event`() = runTest {
        // GIVEN
        whenever(couponCreationFacade.createCoupon())
            .thenReturn(CouponTestUtils.generateTestCoupon(1L, "test"))
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.AddCouponIconClicked)

        // THEN
        verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.CouponsCreateTapped)
    }

    @Test
    fun `given coupon creation returns null, when add coupon icon is tapped, then no event is sent`() = runTest {
        // GIVEN
        whenever(couponCreationFacade.createCoupon()).thenReturn(null)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.AddCouponIconClicked)

        // THEN
        verify(fromChildToParentEventSender, org.mockito.kotlin.never()).sendToParent(any())
    }

    @Test
    fun `when variable product item clicked, then state changes to variation list`() = runTest {
        // GIVEN
        val variableProductData = ItemClickedData.VariableProduct(
            id = 123L,
            name = "Test Variable Product",
            numOfVariations = 5,
            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
        )
        whenever(parentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.ItemClickedInItemsList(variableProductData, null))
        )
        val viewModel = createViewModel()

        // WHEN/THEN
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsToolbarViewState.VariationList::class.java)
            val variationListState = state as WooPosItemsToolbarViewState.VariationList
            assertThat(variationListState.variableProductData.id).isEqualTo(123L)
            assertThat(variationListState.variableProductData.numOfVariations).isEqualTo(5)
            assertThat(variationListState.tabs).hasSize(1)
            val tab = variationListState.tabs.first() as WooPosItemsToolbarViewState.Tab.Variations
            assertThat(tab.name).isEqualTo("Test Variable Product")
            assertThat(tab.highlightLevel).isEqualTo(WooPosItemsToolbarViewState.Tab.HighlightLevel.Full)
        }
    }

    @Test
    fun `when variable product item clicked, then search helper loading state is updated`() = runTest {
        // GIVEN
        val variableProductData = ItemClickedData.VariableProduct(
            id = 123L,
            name = "Test Variable Product",
            numOfVariations = 5,
            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
        )
        whenever(parentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.ItemClickedInItemsList(variableProductData, null))
        )

        // WHEN
        createViewModel()

        // THEN
        verify(searchHelper).updateLoadingState(isLoading = false)
    }

    @Test
    fun `when back from variations clicked, then state changes back to preserved state`() = runTest {
        // GIVEN
        val variableProductData = ItemClickedData.VariableProduct(
            id = 123L,
            name = "Test Variable Product",
            numOfVariations = 5,
            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
        )
        whenever(parentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.ItemClickedInItemsList(variableProductData, null))
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked)

        // THEN
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsToolbarViewState.ProductList::class.java)
        }
    }

    @Test
    fun `when ShowCustomAmountForm received, then state changes to CustomAmountForm`() = runTest {
        // GIVEN
        whenever(parentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.ShowCustomAmountForm())
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsToolbarViewState.CustomAmountForm::class.java)
            assertThat((state as WooPosItemsToolbarViewState.CustomAmountForm).editing).isNull()
        }
    }

    @Test
    fun `when CustomAmountSubmitted received during form, then state restores preserved state`() = runTest {
        // GIVEN
        whenever(parentToChildrenEventReceiver.events).thenReturn(
            flowOf(
                ParentToChildrenEvent.ShowCustomAmountForm(),
                ParentToChildrenEvent.CustomAmountSubmitted(
                    name = "Tip",
                    amount = java.math.BigDecimal("1.00"),
                    isTaxable = false,
                ),
            )
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsToolbarViewState.ProductList::class.java)
        }
    }

    @Test
    fun `given on product list, when navigateBackFromSubScreen path triggers unexpectedly, then state is unchanged`() = runTest {
        // GIVEN — no sub-screen open
        val viewModel = createViewModel()

        // WHEN — back-from-variations event fires while already on the product list
        viewModel.onUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked)

        // THEN — state remains the product list (no crash, no transition)
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsToolbarViewState.ProductList::class.java)
        }
    }

    @Test
    fun `when products tab clicked, then analytics event is tracked`() = runTest {
        // GIVEN
        val productsTab = WooPosItemsToolbarViewState.Tab.Products(
            "Products",
            WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        )
        whenever(tabsHelper.selectTab(any(), eq(productsTab))).thenReturn(tabs)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.OnTabClicked(productsTab))

        // THEN
        val capture = argumentCaptor<WooPosAnalyticsEvent.Event.ItemsHeaderTapped>()
        verify(analyticsTracker).track(capture.capture())
        assertThat(capture.firstValue.name).isEqualTo("items_header_tapped")
        assertThat(capture.firstValue.properties["type"]).isEqualTo(
            WooPosAnalyticsEventConstant.ItemsHeaderType.PRODUCT.value
        )
    }

    @Test
    fun `when single tab present, then tab selection is ignored`() = runTest {
        // GIVEN
        val singleTab = listOf(
            WooPosItemsToolbarViewState.Tab.Products(
                "Products",
                WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
            )
        )
        whenever(tabsHelper.defaultTabs).thenReturn(singleTab)
        val couponsTab = WooPosItemsToolbarViewState.Tab.Coupons(
            "Coupons",
            WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.OnTabClicked(couponsTab))

        // THEN
        viewModel.viewState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(WooPosItemsToolbarViewState.ProductList::class.java)
        }
    }

    @Test
    fun `when switching tabs, then search loading state is reset`() = runTest {
        // GIVEN
        val couponsTab = WooPosItemsToolbarViewState.Tab.Coupons(
            "Coupons",
            WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        )

        whenever(tabsHelper.selectTab(any(), eq(couponsTab))).thenReturn(
            listOf(
                WooPosItemsToolbarViewState.Tab.Products(
                    "Products",
                    WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
                ),
                WooPosItemsToolbarViewState.Tab.Coupons(
                    "Coupons",
                    WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
                )
            )
        )

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.OnTabClicked(couponsTab))

        // THEN
        verify(searchHelper).updateLoadingState(isLoading = false)
    }

    @Test
    fun `when view model created, then wasOpenedOnce is set to true`() = runTest {
        // GIVEN

        // WHEN
        createViewModel()

        // THEN
        verify(preferencesRepository).setWasOpenedOnce(true)
    }

    @Test
    fun `given sync overdue, when view model created, then stale warning tracking event is sent`() = runTest {
        // GIVEN
        val currentTime = 1000000L
        val lastSyncTime = currentTime - (25 * 60 * 60 * 1000L)
        val expectedHours = 25

        whenever(dateTimeProvider.now()).thenReturn(currentTime)
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NonBlockingRequired(lastSyncTime, isOverdue = true)
        )

        // WHEN
        createViewModel()

        // THEN
        verify(analyticsTracker).track(
            eq(WooPosAnalyticsEvent.Event.LocalCatalogStaleWarningShown(expectedHours))
        )
    }

    @Test
    fun `given stale warning shown, when dismiss tapped, then stale warning dismissed event is sent`() = runTest {
        // GIVEN
        val currentTime = 1000000L
        val lastSyncTime = currentTime - (25 * 60 * 60 * 1000L)
        val expectedHours = 25

        whenever(dateTimeProvider.now()).thenReturn(currentTime)
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NonBlockingRequired(lastSyncTime, isOverdue = true)
        )
        val vm = createViewModel()

        // WHEN
        vm.onUIEvent(WooPosItemsUIEvent.SyncOverdueBannerDismissed)
        // THEN
        verify(analyticsTracker).track(
            eq(WooPosAnalyticsEvent.Event.LocalCatalogStaleWarningDismissed)
        )
    }

    @Test
    fun `given sync not overdue, when view model created, then no stale warning tracking event is sent`() = runTest {
        // GIVEN
        whenever(syncStatusChecker.checkSyncRequirement()).thenReturn(
            WooPosFullSyncRequirement.NonBlockingRequired(System.currentTimeMillis(), isOverdue = false)
        )

        // WHEN
        createViewModel()

        // THEN
        verify(analyticsTracker, org.mockito.kotlin.never()).track(
            any<WooPosAnalyticsEvent.Event.LocalCatalogStaleWarningShown>()
        )
    }

    @Test
    fun `given wc version sunset warning required, when view model created, then banner is visible`() = runTest {
        // GIVEN
        whenever(isWooVersionSunsetWarningRequired()).thenReturn(true)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.bannerState.test {
            val state = awaitItem()
            assertThat(state).isEqualTo(WooPosItemsBannerState.WooCommerceVersionSunset)
        }
    }

    @Test
    fun `given wc version sunset warning required, when view model created, then analytics tracked`() = runTest {
        // GIVEN
        whenever(isWooVersionSunsetWarningRequired()).thenReturn(true)

        // WHEN
        createViewModel()

        // THEN
        verify(analyticsTracker).track(eq(WooPosAnalyticsEvent.Event.WooCommerceVersionSunsetWarningShown))
    }

    @Test
    fun `given wc version sunset warning not required, when view model created, then banner is hidden`() = runTest {
        // GIVEN
        whenever(isWooVersionSunsetWarningRequired()).thenReturn(false)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.bannerState.test {
            val state = awaitItem()
            assertThat(state).isEqualTo(WooPosItemsBannerState.Hidden)
        }
    }

    @Test
    fun `given wc version sunset banner visible, when dismissed, then banner is hidden`() = runTest {
        // GIVEN
        whenever(isWooVersionSunsetWarningRequired()).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.WooCommerceVersionSunsetBannerDismissed)

        // THEN
        viewModel.bannerState.test {
            val state = awaitItem()
            assertThat(state).isEqualTo(WooPosItemsBannerState.Hidden)
        }
    }

    @Test
    fun `given wc version sunset banner visible, when dismissed, then dismissal timestamp is saved`() = runTest {
        // GIVEN
        val currentTime = 123456L
        whenever(dateTimeProvider.now()).thenReturn(currentTime)
        whenever(isWooVersionSunsetWarningRequired()).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.WooCommerceVersionSunsetBannerDismissed)

        // THEN
        verify(preferencesRepository).setWooVersionSunsetBannerDismissalTimestamp(currentTime)
    }

    @Test
    fun `given wc version sunset banner visible, when dismissed, then analytics event is tracked`() = runTest {
        // GIVEN
        whenever(isWooVersionSunsetWarningRequired()).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.WooCommerceVersionSunsetBannerDismissed)

        // THEN
        verify(analyticsTracker).track(eq(WooPosAnalyticsEvent.Event.WooCommerceVersionSunsetWarningDismissed))
    }

    private fun createViewModel(): WooPosItemsViewModel {
        return WooPosItemsViewModel(
            searchHelper = searchHelper,
            tabsHelper = tabsHelper,
            couponCreationFacade = couponCreationFacade,
            fromChildToParentEventSender = fromChildToParentEventSender,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            preferencesRepository = preferencesRepository,
            analyticsTracker = analyticsTracker,
            syncStatusChecker = syncStatusChecker,
            dateTimeProvider = dateTimeProvider,
            isWooCommerceVersionSunsetWarningRequired = isWooVersionSunsetWarningRequired,
            resourceProvider = resourceProvider,
        )
    }
}
