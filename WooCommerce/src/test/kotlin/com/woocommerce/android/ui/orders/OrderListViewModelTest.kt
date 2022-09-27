package com.woocommerce.android.ui.orders

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_ORDER_LIST
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.filters.domain.GetSelectedOrderFiltersCount
import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFilters
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.DismissCardReaderUpsellBanner
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.DismissCardReaderUpsellBannerViaDontShowAgain
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.DismissCardReaderUpsellBannerViaRemindMeLater
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.OpenPurchaseCardReaderLink
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.payments.banner.BannerDisplayEligibilityChecker
import com.woocommerce.android.ui.payments.banner.BannerState
import com.woocommerce.android.util.LandscapeChecker
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_ERROR
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_OFFLINE
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_LOADING
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SEARCH_RESULTS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderFetcher
import org.wordpress.android.fluxc.store.WCOrderStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class OrderListViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val orderListRepository: OrderListRepository = mock()
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    private val orderStatusOptions = OrderTestUtils.generateOrderStatusOptionsMappedByStatus()
    private lateinit var viewModel: OrderListViewModel
    private val listStore: ListStore = mock()
    private val pagedListWrapper: PagedListWrapper<OrderListItemUIType> = mock()
    private val orderFetcher: WCOrderFetcher = mock()
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters = mock()
    private val getSelectedOrderFiltersCount: GetSelectedOrderFiltersCount = mock()
    private val bannerDisplayEligibilityChecker: BannerDisplayEligibilityChecker = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val landscapeChecker: LandscapeChecker = mock()

    @Before
    fun setup() = testBlocking {
        whenever(getWCOrderListDescriptorWithFilters.invoke()).thenReturn(WCOrderListDescriptor(site = mock()))
        whenever(pagedListWrapper.listError).doReturn(mock())
        whenever(pagedListWrapper.isEmpty).doReturn(mock())
        whenever(pagedListWrapper.isFetchingFirstPage).doReturn(mock())
        whenever(pagedListWrapper.isLoadingMore).doReturn(mock())
        whenever(pagedListWrapper.data).doReturn(mock())
        whenever(
            listStore.getList<WCOrderListDescriptor, OrderListItemIdentifier, OrderListItemUIType>(
                listDescriptor = any(),
                dataSource = any(),
                lifecycle = any()
            )
        ).doReturn(pagedListWrapper)
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(SiteModel()).whenever(selectedSite).get()

        viewModel = OrderListViewModel(
            savedState = savedStateHandle,
            dispatchers = coroutinesTestRule.testDispatchers,
            orderListRepository = orderListRepository,
            orderDetailRepository = orderDetailRepository,
            orderStore = orderStore,
            listStore = listStore,
            networkStatus = networkStatus,
            dispatcher = dispatcher,
            selectedSite = selectedSite,
            fetcher = orderFetcher,
            resourceProvider = resourceProvider,
            getWCOrderListDescriptorWithFilters = getWCOrderListDescriptorWithFilters,
            getSelectedOrderFiltersCount = getSelectedOrderFiltersCount,
            bannerDisplayEligibilityChecker = bannerDisplayEligibilityChecker,
            orderListTransactionLauncher = mock(),
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            landscapeChecker = landscapeChecker,
        )
    }

    @Test
    fun `Request to load new list fetches order status options and payment gateways if connected`() = testBlocking {
        clearInvocations(orderListRepository)
        viewModel.submitSearchOrFilter(ANY_SEARCH_QUERY)

        verify(viewModel.activePagedListWrapper, times(1))?.fetchFirstPage()
        verify(orderListRepository, times(1)).fetchPaymentGateways()
        verify(orderListRepository, times(1)).fetchOrderStatusOptionsFromApi()
    }

    @Test
    fun `Load orders activates list wrapper`() = testBlocking {
        doReturn(RequestResult.SUCCESS).whenever(orderListRepository).fetchPaymentGateways()

        viewModel.loadOrders()

        assertNotNull(viewModel.ordersPagedListWrapper)
        assertNotNull(viewModel.activePagedListWrapper)
        verify(viewModel.ordersPagedListWrapper, times(1))?.fetchFirstPage()
        verify(viewModel.ordersPagedListWrapper, times(1))?.invalidateData()
        assertEquals(viewModel.ordersPagedListWrapper, viewModel.activePagedListWrapper)
    }

    /**
     * Test for proper handling of a request to fetch orders and order status options
     * when the device is offline. This scenario should result in an "offline" snackbar
     * message being emitted via a [com.woocommerce.android.viewmodel.MultiLiveEvent.Event] and the
     * [OrderListViewModel.viewStateLiveData.isRefreshPending] variable set to true to trigger another
     * attempt once the device comes back online.
     */
    @Test
    fun `Request to fetch order status options while offline handled correctly`() = testBlocking {
        doReturn(false).whenever(networkStatus).isConnected()

        viewModel.fetchOrdersAndOrderDependencies()

        viewModel.event.getOrAwaitValue().let { event ->
            assertTrue(event is ShowErrorSnack)
            assertEquals(event.messageRes, R.string.offline_error)
        }

        var isRefreshPending = false
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isRefreshPending.takeIfNotEqualTo(old?.isRefreshPending) {
                isRefreshPending = it
            }
        }
        assertTrue(isRefreshPending)
    }

    /* Test order status options are emitted via [OrderListViewModel.orderStatusOptions]
    * once fetched, and verify expected methods are called the correct number of
    * times.
    */
    @Test
    fun `Request to fetch order status options emits options`() = testBlocking {
        doReturn(RequestResult.SUCCESS).whenever(orderListRepository).fetchOrderStatusOptionsFromApi()
        doReturn(orderStatusOptions).whenever(orderListRepository).getCachedOrderStatusOptions()

        clearInvocations(orderListRepository)
        viewModel.fetchOrderStatusOptions()

        verify(orderListRepository, times(1)).fetchOrderStatusOptionsFromApi()
        verify(orderListRepository, times(1)).getCachedOrderStatusOptions()
        assertEquals(orderStatusOptions, viewModel.orderStatusOptions.getOrAwaitValue())
    }

    @Test
    fun `Given network is connected, when fetching orders and dependencies, then load order status list from api`() =
        testBlocking {
            doReturn(true).whenever(networkStatus).isConnected()

            viewModel.fetchOrdersAndOrderDependencies()

            verify(orderListRepository).fetchOrderStatusOptionsFromApi()
        }

    /**
     * Test the logic that generates the "No orders yet" empty view for the ALL tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewType].
     *
     * This view gets generated when:
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isError = null
     * - viewModel.orderStatusFilter = ""
     * - viewModel.isSearching = false
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.data != null
     * - There are NO orders in the db for the active store
     */
    @Test
    fun `Display 'No orders yet' empty view when no orders for site for ALL tab`() = testBlocking {
        viewModel.isSearching = false
        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        advanceUntilIdle()

        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, ORDER_LIST)
        }
    }

    /**
     * Test the logic that generates the "error fetching orders" empty list view for any tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewType].
     *
     * This view gets generated when:
     * - viewModel.isSearching = false
     * - viewModel.orderStatusFilter = ""
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.isError = TRUE
     */
    @Test
    fun `Display error empty view on fetch orders error when no cached orders`() = testBlocking {
        viewModel.isSearching = false

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(mock())
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        advanceUntilIdle()

        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, NETWORK_ERROR)
        }
    }

    /**
     * Test the logic that generates the "device offline" empty error list view for any tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewType].
     *
     * This view gets generated when:
     * - networkStatus.isConnected = false
     * - viewModel.isSearching = false
     * - viewModel.orderStatusFilter = ""
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Display offline empty view when offline and list is empty`() = testBlocking {
        viewModel.isSearching = false
        doReturn(false).whenever(networkStatus).isConnected()
        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        advanceUntilIdle()

        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, NETWORK_OFFLINE)
        }
    }

    /**
     * Test the logic that generates the "No matching orders" empty list view for search/filter
     * results is successful and verify the view is emitted via [OrderListViewModel.emptyViewType].
     *
     * This view gets generated when:
     * - viewModel.isSearching = true
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Display empty view for empty search result`() = testBlocking {
        viewModel.isSearching = true
        viewModel.searchQuery = "query"
        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        advanceUntilIdle()

        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, SEARCH_RESULTS)
        }
    }

    /**
     * Test the logic that generates the Loading empty list view for any tab of the order list
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewType].
     *
     * This view gets generated when:
     * - viewModel.isSearching = false
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = true
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Display Loading empty view for any order list tab`() = testBlocking {
        viewModel.isSearching = false
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(true)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        advanceUntilIdle()

        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, ORDER_LIST_LOADING)
        }
    }

    /**
     * Test the logic that generates the Loading empty list view while in search mode
     * and verify the empty view is *not* shown in this situation
     *
     * This view gets generated when:
     * - viewModel.isSearching = true
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = true
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Does not display the Loading empty view in search mode`() = testBlocking {
        viewModel.isSearching = true
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(true)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        advanceUntilIdle()
        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNull(emptyView)
        }
    }

    @Test
    fun `Payment gateways are fetched if network connected and variable set when successful`() = testBlocking {
        doReturn(RequestResult.SUCCESS).whenever(orderListRepository).fetchPaymentGateways()

        viewModel.fetchPaymentGateways()

        verify(orderListRepository, times(1)).fetchPaymentGateways()
        assertTrue(viewModel.viewState.arePaymentGatewaysFetched)
    }

    @Test
    fun `Payment gateways are not fetched if network not connected`() = testBlocking {
        doReturn(false).whenever(networkStatus).isConnected()

        viewModel.fetchPaymentGateways()

        verify(orderListRepository, times(0)).fetchPaymentGateways()
        assertFalse(viewModel.viewState.arePaymentGatewaysFetched)
    }

    @Test
    fun `Payment gateways are not fetched if already fetched and network connected`() = testBlocking {
        doReturn(RequestResult.SUCCESS).whenever(orderListRepository).fetchPaymentGateways()

        // Fetch the first time around
        viewModel.fetchPaymentGateways()
        verify(orderListRepository, times(1)).fetchPaymentGateways()
        assertTrue(viewModel.viewState.arePaymentGatewaysFetched)
        clearInvocations(orderListRepository)

        // Try to fetch a second time
        viewModel.fetchPaymentGateways()
        verify(orderListRepository, times(0)).fetchPaymentGateways()
        assertTrue(viewModel.viewState.arePaymentGatewaysFetched)
    }

    /**
     * Ideally, this shouldn't be required as NotificationMessageHandler.dispatchBackgroundEvents
     * dispatches events that will trigger fetching orders and updating UI state.
     *
     * This doesn't work for search queries though as they use custom [WCOrderListDescriptor]
     * which contains a search query and based on this UI is refreshed or not.
     *
     * ATM we'll just trigger [PagedListWrapper.fetchFirstPage]. It's not an issue as later
     * in the flow we use [WCOrderFetcher] which filters out requests that duplicate requests
     * of fetching order.
     */
    @Test
    fun `Request refresh for active list when received new order notification and is in search`() = testBlocking {
        viewModel.isSearching = true

        viewModel.submitSearchOrFilter(searchQuery = "Joe Doe")

        // Reset as we're no interested in previous invocations in this test
        reset(viewModel.activePagedListWrapper)
        viewModel.onNotificationReceived(
            NotificationReceivedEvent(NotificationChannelType.NEW_ORDER)
        )

        verify(viewModel.activePagedListWrapper)?.fetchFirstPage()
    }

    //region Card Reader Upsell
    @Test
    fun `given upsell banner, when purchase reader clicked, then trigger proper event`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(KEY_BANNER_ORDER_LIST)
            ).thenReturn(
                "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
            )
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            viewModel.updateBannerState(landscapeChecker, false)

            // WHEN
            viewModel.bannerState.value?.onPrimaryActionClicked?.invoke()

            // Then
            assertThat(
                viewModel.event.value
            ).isInstanceOf(OpenPurchaseCardReaderLink::class.java)
        }
    }

    @Test
    fun `given landscape mode, when viewmodel init, then do not display banner`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            whenever(landscapeChecker.isLandscape()).thenReturn(true)

            // WHEN
            viewModel.updateBannerState(landscapeChecker, false)

            // Then
            assertThat(
                (viewModel.bannerState.value as BannerState).shouldDisplayBanner
            ).isFalse
        }
    }

    @Test
    fun `given portrait mode, when viewmodel init, then display the banner`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            whenever(landscapeChecker.isLandscape()).thenReturn(false)

            // WHEN
            viewModel.updateBannerState(landscapeChecker, false)

            // Then
            assertThat(
                (viewModel.bannerState.value as BannerState).shouldDisplayBanner
            ).isTrue
        }
    }

    @Test
    fun `given 0 orders, when viewmodel init, then do not display the banner`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)

            // WHEN
            viewModel.updateBannerState(landscapeChecker, true)

            // Then
            assertThat(
                (viewModel.bannerState.value as BannerState).shouldDisplayBanner
            ).isFalse
        }
    }

    @Test
    fun `given more than 0 orders, when viewmodel init, then display the banner`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)

            // WHEN
            viewModel.updateBannerState(landscapeChecker, false)

            // Then
            assertThat(
                (viewModel.bannerState.value as BannerState).shouldDisplayBanner
            ).isTrue
        }
    }

    @Test
    fun `given upsell banner, when banner is dismissed, then trigger DismissCardReaderUpsellBanner event`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            viewModel.updateBannerState(landscapeChecker, false)

            // WHEN
            viewModel.bannerState.value?.onDismissClicked?.invoke()

            // Then
            assertThat(
                viewModel.event.value
            ).isEqualTo(DismissCardReaderUpsellBanner)
        }
    }

    @Test
    fun `given upsell banner, when banner is dismissed via remind later, then trigger proper event`() {
        // WHEN
        viewModel.onRemindLaterClicked(0L, KEY_BANNER_ORDER_LIST)

        // Then
        assertThat(viewModel.event.value).isEqualTo(
            DismissCardReaderUpsellBannerViaRemindMeLater
        )
    }

    @Test
    fun `given upsell banner, when banner is dismissed via don't show again, then trigger proper event`() {
        // WHEN
        viewModel.onDontShowAgainClicked(KEY_BANNER_ORDER_LIST)

        // Then
        assertThat(viewModel.event.value).isEqualTo(
            DismissCardReaderUpsellBannerViaDontShowAgain
        )
    }

    @Test
    fun `given card reader banner has dismissed, then update dialogShow state to true`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            viewModel.updateBannerState(landscapeChecker, false)

            // WHEN
            viewModel.bannerState.value?.onDismissClicked?.invoke()

            // Then
            assertThat(
                viewModel.event.value
            ).isEqualTo(DismissCardReaderUpsellBanner)

            assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isTrue
        }
    }

    @Test
    fun `given card reader banner has dismissed via remind later, then update dialogShow state to false`() {
        viewModel.onRemindLaterClicked(0L, KEY_BANNER_ORDER_LIST)

        assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isFalse
    }

    @Test
    fun `given card reader banner has dismissed via don't show again, then update dialogShow state to false`() {
        viewModel.onDontShowAgainClicked(KEY_BANNER_ORDER_LIST)

        assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isFalse
    }

    @Test
    fun `given view model init, then update dialogShow state to false`() {
        assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isFalse
    }

    @Test
    fun `given store not eligible for IPP, then do not display banner`() {
        runTest {
            // GIVEN
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(false)

            // WHEN
            viewModel.updateBannerState(landscapeChecker, false)

            // THEN
            assertFalse(viewModel.bannerState.value?.shouldDisplayBanner!!)
        }
    }

    @Test
    fun `given store eligible for IPP and banner condition satisfied, then display banner`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)

            // WHEN
            viewModel.updateBannerState(landscapeChecker, false)

            // THEN
            assertTrue(viewModel.bannerState.value?.shouldDisplayBanner!!)
        }
    }

    @Test
    fun `when alert dialog dismissed by pressing back, then shouldShowUpsellCardReaderDismissDialog set to false`() {
        viewModel.onBannerAlertDismiss()

        assertThat(viewModel.shouldShowUpsellCardReaderDismissDialog.value).isFalse
    }

    @Test
    fun `when upsell card reader banner is displayed, then don't display feedback banner`() {
        runTest {
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)

            val shouldDisplaySimplePaymentsWIPCard = viewModel.shouldDisplaySimplePaymentsWIPCard()

            assertFalse(shouldDisplaySimplePaymentsWIPCard)
        }
    }

    @Test
    fun `when upsell card reader banner is not displayed, then display feedback banner`() {
        runTest {
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(false)

            val shouldDisplaySimplePaymentsWIPCard = viewModel.shouldDisplaySimplePaymentsWIPCard()

            assertTrue(shouldDisplaySimplePaymentsWIPCard)
        }
    }

    @Test
    fun `given banner is displayed, when primary action is invoked, then correct source is tracked`() {
        runTest {
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            viewModel.updateBannerState(landscapeChecker, false)
            val captor = argumentCaptor<String>()

            // WHEN
            viewModel.bannerState.value?.onPrimaryActionClicked?.invoke()

            verify(bannerDisplayEligibilityChecker).getPurchaseCardReaderUrl(captor.capture())
            assertThat(captor.firstValue).isEqualTo(KEY_BANNER_ORDER_LIST)
        }
    }

    @Test
    fun `given banner is displayed, then correct title is displayed`() {
        runTest {
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            viewModel.updateBannerState(landscapeChecker, false)

            // WHEN
            val title = viewModel.bannerState.value?.title

            assertThat(title).isEqualTo(R.string.card_reader_upsell_card_reader_banner_title)
        }
    }

    @Test
    fun `given banner is displayed, then correct description is displayed`() {
        runTest {
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            viewModel.updateBannerState(landscapeChecker, false)

            // WHEN
            val description = viewModel.bannerState.value?.description

            assertThat(description).isEqualTo(R.string.card_reader_upsell_card_reader_banner_description)
        }
    }

    @Test
    fun `given banner is displayed, then correct primary action label is displayed`() {
        runTest {
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            viewModel.updateBannerState(landscapeChecker, false)

            // WHEN
            val primaryActionLabel = viewModel.bannerState.value?.primaryActionLabel

            assertThat(primaryActionLabel).isEqualTo(R.string.card_reader_upsell_card_reader_banner_cta)
        }
    }

    @Test
    fun `given banner is displayed, then correct chip label is displayed`() {
        runTest {
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            viewModel.updateBannerState(landscapeChecker, false)

            // WHEN
            val chipLabel = viewModel.bannerState.value?.chipLabel

            assertThat(chipLabel).isEqualTo(R.string.card_reader_upsell_card_reader_banner_new)
        }
    }

    @Test
    fun `given banner is displayed, then track banner shown event`() {
        runTest {
            whenever(
                bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(anyLong())
            ).thenReturn(true)
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)
            viewModel.updateBannerState(landscapeChecker, false)

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.FEATURE_CARD_SHOWN,
                mapOf(
                    AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_ORDER_LIST,
                    AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS
                )
            )
        }
    }
    //endregion

    @Test
    fun `when the order is swiped then the status is changed optimistically`() = testBlocking {
        // Given that updateOrderStatus will success
        val order = OrderTestUtils.generateOrder()
        val position = 1
        val gesture = OrderStatusUpdateSource.SwipeToCompleteGesture(order.orderId, position, order.status)
        val result = WCOrderStore.OnOrderChanged()

        val updateFlow = flow {
            emit(WCOrderStore.UpdateOrderResult.OptimisticUpdateResult(WCOrderStore.OnOrderChanged()))
            delay(1_000)
            emit(WCOrderStore.UpdateOrderResult.RemoteUpdateResult(result))
        }

        whenever(resourceProvider.getString(R.string.orderlist_mark_completed_success, order.orderId))
            .thenReturn("Order #${order.orderId} marked as completed")
        whenever(orderDetailRepository.updateOrderStatus(order.orderId, CoreOrderStatus.COMPLETED.value))
            .thenReturn(updateFlow)

        // When the order is swiped
        viewModel.onSwipeStatusUpdate(gesture)

        // Then the order status is changed optimistically
        val optimisticChangeEvent = viewModel.event.getOrAwaitValue()
        assertTrue(optimisticChangeEvent is MultiLiveEvent.Event.ShowUndoSnackbar)

        advanceTimeBy(1_001)

        // Then when the order status changed nothing happens because it was already handled optimistically
        val resultEvent = viewModel.event.getOrAwaitValue()
        assertEquals(optimisticChangeEvent, resultEvent)
    }

    @Test
    fun `when the order is swiped but the change fails, then a retry message is shown`() = testBlocking {
        // Given that updateOrderStatus will fail
        val order = OrderTestUtils.generateOrder()
        val position = 1
        val gesture = OrderStatusUpdateSource.SwipeToCompleteGesture(order.orderId, position, order.status)
        val result = WCOrderStore.OnOrderChanged(orderError = WCOrderStore.OrderError())

        val updateFlow = flow {
            emit(WCOrderStore.UpdateOrderResult.OptimisticUpdateResult(WCOrderStore.OnOrderChanged()))
            delay(1_000)
            emit(WCOrderStore.UpdateOrderResult.RemoteUpdateResult(result))
        }

        whenever(resourceProvider.getString(R.string.orderlist_mark_completed_success, order.orderId))
            .thenReturn("Order #${order.orderId} marked as completed")
        whenever(resourceProvider.getString(R.string.orderlist_updating_order_error, order.orderId))
            .thenReturn("Error updating Order #${order.orderId}")
        whenever(orderDetailRepository.updateOrderStatus(order.orderId, CoreOrderStatus.COMPLETED.value))
            .thenReturn(updateFlow)

        // When the order is swiped
        viewModel.onSwipeStatusUpdate(gesture)

        // Then the order status is changed optimistically
        val optimisticChangeEvent = viewModel.event.getOrAwaitValue()
        assertTrue(optimisticChangeEvent is MultiLiveEvent.Event.ShowUndoSnackbar)

        advanceTimeBy(1_001)

        // Then when the order status change fails, the retry message is shown
        val resultEvent = viewModel.event.getOrAwaitValue()
        assertTrue(resultEvent is OrderListViewModel.OrderListEvent.ShowRetryErrorSnack)
    }

    private companion object {
        const val ANY_SEARCH_QUERY = "search query"
    }
}
