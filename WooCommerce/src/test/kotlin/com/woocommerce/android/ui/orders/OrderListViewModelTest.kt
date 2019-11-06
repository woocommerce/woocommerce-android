package com.woocommerce.android.ui.orders

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.list.OrderListEmptyUiState
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.TEST_DISPATCHER
import com.woocommerce.android.viewmodel.test
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@UseExperimental(InternalCoroutinesApi::class)
class OrderListViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val repository: OrderListRepository = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()

    private val orders = OrderTestUtils.generateOrders()
    private val orderStatusOptions = OrderTestUtils.generateOrderStatusOptionsMappedByStatus()
    private lateinit var viewModel: OrderListViewModel
    private val listStore: ListStore = mock()
    private val pagedListWrapper: PagedListWrapper<OrderListItemUIType> = mock()

    @Before
    fun setup() {
        whenever(pagedListWrapper.listError).doReturn(mock())
        whenever(pagedListWrapper.isEmpty).doReturn(mock())
        whenever(pagedListWrapper.isFetchingFirstPage).doReturn(mock())
        whenever(pagedListWrapper.isLoadingMore).doReturn(mock())
        whenever(pagedListWrapper.data).doReturn(mock())
        whenever(listStore.getList<WCOrderListDescriptor, OrderListItemIdentifier, OrderListItemUIType>(
                listDescriptor = any(),
                dataSource = any(),
                lifecycle = any()
        )).doReturn(pagedListWrapper)

        viewModel = spy(
            OrderListViewModel(
                mainDispatcher = TEST_DISPATCHER,
                bgDispatcher = TEST_DISPATCHER,
                repository = repository,
                orderStore = orderStore,
                listStore = listStore,
                networkStatus = networkStatus,
                dispatcher = dispatcher,
                selectedSite = selectedSite
            )
        )

        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(SiteModel()).whenever(selectedSite).get()
    }

    /**
     * Test order status options are fetched and state-related variables are
     * properly set when [OrderListViewModel.start] is called.
     */
    @Test
    fun `Order status options fetched and emitted on start`() = test {
        doReturn(orderStatusOptions).whenever(repository).getCachedOrderStatusOptions()

        // calling this method should populate order status options from cache
        viewModel.start()

        verify(repository, times(1)).getCachedOrderStatusOptions()
        assertTrue(viewModel.isStarted)
        assertEquals(viewModel.orderStatusOptions.getOrAwaitValue(), orderStatusOptions)
    }

    /**
     * Test order status options are emitted via [OrderListViewModel.orderStatusOptions]
     * once fetched, and verify expected methods are called the correct number of
     * times.
     */
    @Test
    fun `Request to fetch order status options emits options`() = test {
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderStatusOptionsFromApi()
        doReturn(orderStatusOptions).whenever(repository).getCachedOrderStatusOptions()

        viewModel.loadList()

        assertNotNull(viewModel.pagedListWrapper)
        verify(viewModel.pagedListWrapper, times(1))?.fetchFirstPage()
        verify(repository, times(1)).fetchOrderStatusOptionsFromApi()
        verify(repository, times(1)).getCachedOrderStatusOptions()
        assertEquals(viewModel.orderStatusOptions.getOrAwaitValue(), orderStatusOptions)
    }

    /**
     * Test for proper handling of a request to fetch orders and order status options
     * when the device is offline. This scenario should result in an "offline" snackbar
     * message being emitted via [OrderListViewModel.showSnackbarMessage] and the
     * [OrderListViewModel.isRefreshPending] variable set to true to trigger another
     * attempt once the device comes back online.
     */
    @Test
    fun `Request to fetch order status options while offline handled correctly`() = test {
        doReturn(false).whenever(networkStatus).isConnected()

        viewModel.fetchOrdersAndOrderStatusOptions()

        assertNull(viewModel.pagedListWrapper)
        assertEquals(viewModel.showSnackbarMessage.getOrAwaitValue(), R.string.offline_error)
        assertTrue(viewModel.isRefreshPending)
    }

    /**
     * Test the logic that generates the "No orders yet" empty view for the ALL tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewState].
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
    fun `Display |No orders yet| empty view when no orders for site for ALL tab`() = test {
        whenever(viewModel.isSearching).doReturn(false)
        doReturn(true).whenever(repository).hasCachedOrdersForSite()

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyUiState(pagedListWrapper)
        viewModel.emptyViewState.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewState.value
            assertNotNull(emptyView)
            assertTrue(emptyView.emptyViewVisible)
            assertTrue(emptyView is OrderListEmptyUiState.EmptyList)
            assertEquals(emptyView.title, UiStringRes(string.orders_empty_message_with_filter))
            assertEquals(emptyView.imgResId, R.drawable.ic_hourglass_empty)
        }
    }

    /**
     * Test the logic that generates the "No orders to process yet" empty view for the PROCESSING tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewState].
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
    fun `Display |No orders to process yet| empty view when no orders for site for PROCESSING tab`() = test {
        whenever(viewModel.isSearching).doReturn(false)
        whenever(viewModel.orderStatusFilter).doReturn(CoreOrderStatus.PROCESSING.value)
        doReturn(false).whenever(repository).hasCachedOrdersForSite()

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyUiState(pagedListWrapper)
        viewModel.emptyViewState.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewState.value
            assertNotNull(emptyView)
            assertTrue(emptyView.emptyViewVisible)
            assertTrue(emptyView is OrderListEmptyUiState.EmptyList)
            assertEquals(emptyView.title, UiStringRes(string.orders_empty_message_with_processing))
            assertEquals(emptyView.imgResId, R.drawable.ic_hourglass_empty)
        }
    }

    /**
     * Test the logic that generates the "All orders processed" empty list view for the PROCESSING tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewState].
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
    fun `Processing Tab displays |All orders processed| empty view if no orders to process`() = test {
        whenever(viewModel.isSearching).doReturn(false)
        whenever(viewModel.orderStatusFilter).doReturn(CoreOrderStatus.PROCESSING.value)
        doReturn(true).whenever(repository).hasCachedOrdersForSite()

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyUiState(pagedListWrapper)
        viewModel.emptyViewState.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewState.value
            assertNotNull(emptyView)
            assertTrue(emptyView.emptyViewVisible)
            assertTrue(emptyView is OrderListEmptyUiState.EmptyList)
            assertEquals(emptyView.title, UiStringRes(string.orders_processed_empty_message))
            assertEquals(emptyView.imgResId, R.drawable.ic_gridicons_checkmark)
        }
    }

    /**
     * Test the logic that generates the "error fetching orders" empty list view for any tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewState].
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
        whenever(viewModel.isSearching).doReturn(false)
        whenever(viewModel.orderStatusFilter).doReturn(StringUtils.EMPTY)

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(mock())
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyUiState(pagedListWrapper)
        viewModel.emptyViewState.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewState.value
            assertNotNull(emptyView)
            assertTrue(emptyView.emptyViewVisible)
            assertTrue(emptyView is OrderListEmptyUiState.ErrorWithRetry)
            assertEquals(emptyView.title, UiStringRes(string.orderlist_error_fetch_generic))
            assertEquals(emptyView.buttonText, UiStringRes(string.retry))
        }
    }

    /**
     * Test the logic that generates the "device offline" empty error list view for any tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewState].
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
        whenever(viewModel.isSearching).doReturn(false)
        whenever(viewModel.orderStatusFilter).doReturn(StringUtils.EMPTY)
        doReturn(false).whenever(networkStatus).isConnected()

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyUiState(pagedListWrapper)
        viewModel.emptyViewState.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewState.value
            assertNotNull(emptyView)
            assertTrue(emptyView.emptyViewVisible)
            assertTrue(emptyView is OrderListEmptyUiState.ErrorWithRetry)
            assertEquals(emptyView.title, UiStringRes(string.error_generic_network))
            assertEquals(emptyView.buttonText, UiStringRes(string.retry))
        }
    }

    /**
     * Test the logic that generates the "No matching orders" empty list view for search/filter
     * results is successful and verify the view is emitted via [OrderListViewModel.emptyViewState].
     *
     * This view gets generated when:
     * - viewModel.isSearching = true
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Display |No matching orders| for empty search result`() = test {
        whenever(viewModel.isSearching).doReturn(true)

        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)

        viewModel.createAndPostEmptyUiState(pagedListWrapper)
        viewModel.emptyViewState.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewState.value
            assertNotNull(emptyView)
            assertTrue(emptyView.emptyViewVisible)
            assertTrue(emptyView is OrderListEmptyUiState.EmptyList)
            assertEquals(emptyView.title, UiStringRes(string.orders_empty_message_with_search))
            assertEquals(emptyView.imgResId, null)
        }
    }

    /**
     * Test the logic that generates the Loading empty list view for any tab of the order list
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewState].
     *
     * This view gets generated when:
     * - viewModel.isSearching = false
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = true
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Display Loading empty view for any order list tab`() = test {
        whenever(viewModel.isSearching).doReturn(false)

        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(true)

        viewModel.createAndPostEmptyUiState(pagedListWrapper)
        viewModel.emptyViewState.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewState.value
            assertNotNull(emptyView)
            assertTrue(emptyView.emptyViewVisible)
            assertTrue(emptyView is OrderListEmptyUiState.Loading)
        }
    }

    /**
     * Test the logic that generates the Loading empty list view while in search mode
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewState].
     * Since search mode displays a list of order statuses, an empty view should not be shown
     * so the logic should return the [OrderListEmptyUiState.DataShown] to hide the empty view.
     *
     * This view gets generated when:
     * - viewModel.isSearching = true
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = true
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Does not display the Loading empty view in search mode`() = test {
        whenever(viewModel.isSearching).doReturn(true)

        whenever(pagedListWrapper.listError.value).doReturn(null)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(true)

        viewModel.createAndPostEmptyUiState(pagedListWrapper)
        viewModel.emptyViewState.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewState.value
            assertNotNull(emptyView)
            assertFalse(emptyView.emptyViewVisible)
            assertTrue(emptyView is OrderListEmptyUiState.DataShown)
        }
    }
}
