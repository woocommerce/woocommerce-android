package com.woocommerce.android.ui.orders

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.filters.domain.GetSelectedOrderFiltersCount
import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFilters
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderFetcher
import org.wordpress.android.fluxc.store.WCOrderStore
import kotlin.test.*

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class OrderListViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val orderListRepository: OrderListRepository = mock()
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
    private val appPrefsWrapper: AppPrefsWrapper = mock()

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
            orderStore = orderStore,
            listStore = listStore,
            networkStatus = networkStatus,
            dispatcher = dispatcher,
            selectedSite = selectedSite,
            fetcher = orderFetcher,
            resourceProvider = resourceProvider,
            appPrefsWrapper = appPrefsWrapper,
            getWCOrderListDescriptorWithFilters = getWCOrderListDescriptorWithFilters,
            getSelectedOrderFiltersCount = getSelectedOrderFiltersCount
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

    private companion object {
        const val ANY_SEARCH_QUERY = "search query"
    }
}
