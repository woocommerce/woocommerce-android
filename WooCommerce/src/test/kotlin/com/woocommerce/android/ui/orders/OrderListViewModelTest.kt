package com.woocommerce.android.ui.orders

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.list.OrderFetcher
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.OrderListViewModel.ViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.TestDispatcher
import com.woocommerce.android.viewmodel.test
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_ERROR
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_OFFLINE
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_ALL_PROCESSED
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_LOADING
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SEARCH_RESULTS
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@InternalCoroutinesApi
class OrderListViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val repository: OrderListRepository = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val coroutineDispatchers = CoroutineDispatchers(
        TestDispatcher,
        TestDispatcher,
        TestDispatcher
    )
    private val savedStateArgs: SavedStateWithArgs = mock()

    private val orderStatusOptions = OrderTestUtils.generateOrderStatusOptionsMappedByStatus()
    private lateinit var viewModel: OrderListViewModel
    private val listStore: ListStore = mock()
    private val pagedListWrapper: PagedListWrapper<OrderListItemUIType> = mock()
    private val orderFetcher: OrderFetcher = mock()
    private val wooCommerceStore: WooCommerceStore = mock()

    @Before
    fun setup() = test {
        whenever(pagedListWrapper.listError).doReturn(mock())
        whenever(pagedListWrapper.isEmpty).doReturn(mock())
        whenever(pagedListWrapper.isFetchingFirstPage).doReturn(mock())
        whenever(pagedListWrapper.isLoadingMore).doReturn(mock())
        whenever(pagedListWrapper.data).doReturn(mock())
        whenever(pagedListWrapper.listChanged).doReturn(mock())
        whenever(listStore.getList<WCOrderListDescriptor, OrderListItemIdentifier, OrderListItemUIType>(
                listDescriptor = any(),
                dataSource = any(),
                lifecycle = any()
        )).doReturn(pagedListWrapper)
        doReturn(orderStatusOptions).whenever(repository).getCachedOrderStatusOptions()
        doReturn(MutableLiveData(ViewState())).whenever(savedStateArgs).getLiveData<ViewState>(any(), any())
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(SiteModel()).whenever(selectedSite).get()

        viewModel = OrderListViewModel(
                savedState = savedStateArgs,
                coroutineDispatchers = coroutineDispatchers,
                repository = repository,
                orderStore = orderStore,
                listStore = listStore,
                networkStatus = networkStatus,
                dispatcher = dispatcher,
                selectedSite = selectedSite,
                fetcher = orderFetcher,
                resourceProvider = resourceProvider,
                wooCommerceStore = wooCommerceStore
        )
    }

    /**
     * Test cached order status options are fetched from the db when the
     * ViewModel is initialized. Since the ViewModel is initialized during the
     * [setup] method, there is nothing to do but verify everything here.
     */
    @Test
    fun `Cached order status options fetched and emitted during initialization`() = test {
        verify(repository, times(1)).getCachedOrderStatusOptions()
        assertEquals(viewModel.orderStatusOptions.getOrAwaitValue(), orderStatusOptions)
    }

    @Test
    fun `request to load new list fetches order status options and payment gateways if connected`() = test {
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderStatusOptionsFromApi()
        doReturn(orderStatusOptions).whenever(repository).getCachedOrderStatusOptions()
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchPaymentGateways()

        clearInvocations(repository)
        viewModel.submitSearchOrFilter()

        verify(viewModel.activePagedListWrapper, times(1))?.fetchFirstPage()
        verify(repository, times(1)).fetchOrderStatusOptionsFromApi()
        verify(repository, times(1)).getCachedOrderStatusOptions()
        verify(repository, times(1)).fetchPaymentGateways()
    }

    @Test
    fun `load orders for ALL tab activates list wrapper`() = test {
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderStatusOptionsFromApi()
        doReturn(orderStatusOptions).whenever(repository).getCachedOrderStatusOptions()
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchPaymentGateways()

        viewModel.initializeListsForMainTabs()
        viewModel.loadAllList()

        assertNotNull(viewModel.allPagedListWrapper)
        assertNotNull(viewModel.activePagedListWrapper)
        verify(viewModel.allPagedListWrapper, times(2))?.fetchFirstPage()
        verify(viewModel.allPagedListWrapper, times(1))?.invalidateData()
        assertEquals(viewModel.allPagedListWrapper, viewModel.activePagedListWrapper)
    }

    @Test
    fun `load orders for ALL tab after initial run does not fetch first page`() = test {
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderStatusOptionsFromApi()
        doReturn(orderStatusOptions).whenever(repository).getCachedOrderStatusOptions()
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchPaymentGateways()

        viewModel.initializeListsForMainTabs()
        clearInvocations(viewModel.allPagedListWrapper)
        viewModel.loadAllList()

        assertNotNull(viewModel.allPagedListWrapper)
        assertNotNull(viewModel.activePagedListWrapper)
        verify(viewModel.allPagedListWrapper, times(0))?.fetchFirstPage()
        verify(viewModel.allPagedListWrapper, times(1))?.invalidateData()
        assertEquals(viewModel.allPagedListWrapper, viewModel.activePagedListWrapper)
    }

    @Test
    fun `load orders for PROCESSING activates list wrapper and fetches first page`() = test {
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderStatusOptionsFromApi()
        doReturn(orderStatusOptions).whenever(repository).getCachedOrderStatusOptions()
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchPaymentGateways()

        viewModel.initializeListsForMainTabs()
        clearInvocations(repository)
        clearInvocations(viewModel.processingPagedListWrapper)
        viewModel.loadProcessingList()

        assertNotNull(viewModel.processingPagedListWrapper)
        assertNotNull(viewModel.activePagedListWrapper)
        verify(viewModel.processingPagedListWrapper, times(0))?.fetchFirstPage()
        verify(viewModel.processingPagedListWrapper, times(1))?.invalidateData()
        assertEquals(viewModel.processingPagedListWrapper, viewModel.activePagedListWrapper)
    }

    @Test
    fun `load orders for PROCESSING tab after initial run does not fetch first page`() = test {
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderStatusOptionsFromApi()
        doReturn(orderStatusOptions).whenever(repository).getCachedOrderStatusOptions()
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchPaymentGateways()

        viewModel.initializeListsForMainTabs()
        viewModel.loadProcessingList()
        clearInvocations(viewModel.processingPagedListWrapper)
        viewModel.loadProcessingList()

        assertNotNull(viewModel.processingPagedListWrapper)
        assertNotNull(viewModel.activePagedListWrapper)
        verify(viewModel.processingPagedListWrapper, times(0))?.fetchFirstPage()
        verify(viewModel.processingPagedListWrapper, times(1))?.invalidateData()
        assertEquals(viewModel.processingPagedListWrapper, viewModel.activePagedListWrapper)
    }

    /**
     * Test order status options are emitted via [OrderListViewModel.orderStatusOptions]
     * once fetched, and verify expected methods are called the correct number of
     * times.
     */
    @Test
    fun `Request to fetch order status options emits options`() = test {
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderStatusOptionsFromApi()

        clearInvocations(repository)
        viewModel.fetchOrderStatusOptions()

        verify(repository, times(1)).fetchOrderStatusOptionsFromApi()
        verify(repository, times(1)).getCachedOrderStatusOptions()
        assertEquals(viewModel.orderStatusOptions.getOrAwaitValue(), orderStatusOptions)
    }

    /**
     * Test for proper handling of a request to fetch orders and order status options
     * when the device is offline. This scenario should result in an "offline" snackbar
     * message being emitted via a [com.woocommerce.android.viewmodel.MultiLiveEvent.Event] and the
     * [OrderListViewModel.viewStateLiveData.isRefreshPending] variable set to true to trigger another
     * attempt once the device comes back online.
     */
    @Test
    fun `Request to fetch order status options while offline handled correctly`() = test {
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
    fun `Display 'No orders yet' empty view when no orders for site for ALL tab`() = test {
        viewModel.isSearching = false
        doReturn(true).whenever(repository).hasCachedOrdersForSite()

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, ORDER_LIST)
        }
    }

    /**
     * Test the logic that generates the "No orders to process yet" empty view for the PROCESSING tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewType].
     *
     * This view gets generated when:
     * - viewModel.isSearching = false
     * - viewModel.orderStatusFilter = "processing"
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.isError = null
     * - pagedListWrapper.data != null
     * - There are NO orders in the db for the active store
     */
    @Test
    fun `Display 'No orders to process yet' empty view when no orders for site for PROCESSING tab`() = test {
        viewModel.isSearching = false
        viewModel.orderStatusFilter = CoreOrderStatus.PROCESSING.value
        doReturn(true).whenever(repository).hasCachedOrdersForSite()

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, ORDER_LIST_ALL_PROCESSED)
        }
    }

    /**
     * Test the logic that generates the "All orders processed" empty list view for the PROCESSING tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewType].
     *
     * This view gets generated when:
     * - viewModel.isSearching = false
     * - viewModel.orderStatusFilter = "processing"
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.isError = null
     * - There is 1 or more orders in the db for the active store
     */
    @Test
    fun `Processing Tab displays 'All orders processed' empty view if no orders to process`() = test {
        viewModel.isSearching = false
        viewModel.orderStatusFilter = CoreOrderStatus.PROCESSING.value
        doReturn(true).whenever(repository).hasCachedOrdersForSite()

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, ORDER_LIST_ALL_PROCESSED)
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
    fun `Display error empty view on fetch orders error when no cached orders`() = test {
        viewModel.isSearching = false
        viewModel.orderStatusFilter = StringUtils.EMPTY

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(mock())
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
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
    fun `Display offline empty view when offline and list is empty`() = test {
        viewModel.isSearching = false
        viewModel.orderStatusFilter = StringUtils.EMPTY
        doReturn(false).whenever(networkStatus).isConnected()
        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
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
    fun `Display empty view for empty search result`() = test {
        viewModel.isSearching = true
        viewModel.searchQuery = "query"
        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
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
    fun `Display Loading empty view for any order list tab`() = test {
        viewModel.isSearching = false
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(true)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
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
    fun `Does not display the Loading empty view in search mode`() = test {
        viewModel.isSearching = true
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(true)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNull(emptyView)
        }
    }

    @Test
    fun `Payment gateways are fetched if network connected and variable set when successful`() = test {
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchPaymentGateways()

        viewModel.fetchPaymentGateways()

        verify(repository, times(1)).fetchPaymentGateways()
        assertTrue(viewModel.viewState.arePaymentGatewaysFetched)
    }

    @Test
    fun `Payment gateways are not fetched if network not connected`() = test {
        doReturn(false).whenever(networkStatus).isConnected()

        viewModel.fetchPaymentGateways()

        verify(repository, times(0)).fetchPaymentGateways()
        assertFalse(viewModel.viewState.arePaymentGatewaysFetched)
    }

    @Test
    fun `Payment gateways are not fetched if already fetched and network connected`() = test {
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchPaymentGateways()

        // Fetch the first time around
        viewModel.fetchPaymentGateways()
        verify(repository, times(1)).fetchPaymentGateways()
        assertTrue(viewModel.viewState.arePaymentGatewaysFetched)
        clearInvocations(repository)

        // Try to fetch a second time
        viewModel.fetchPaymentGateways()
        verify(repository, times(0)).fetchPaymentGateways()
        assertTrue(viewModel.viewState.arePaymentGatewaysFetched)
    }
}
