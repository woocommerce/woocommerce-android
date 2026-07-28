package com.woocommerce.android.ui.orders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagedList
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppConstants
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.notifications.ShowTestNotification
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningTracker
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.CodeScanningErrorType
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.ScanningSource
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.filters.domain.GetSelectedOrderFiltersCount
import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFilters
import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFiltersAndSearchQuery
import com.woocommerce.android.ui.orders.list.BulkUpdateOrderResult
import com.woocommerce.android.ui.orders.list.FetchOrdersRepository
import com.woocommerce.android.ui.orders.list.ObserveOrdersListLastUpdate
import com.woocommerce.android.ui.orders.list.OrderListFragmentArgs
import com.woocommerce.android.ui.orders.list.OrderListItemDataSource
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.orders.list.OrderListViewModel.Companion.BULK_UPDATE_COUNT_LIMIT
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.OnAddingProductViaScanningFailed
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.ShouldUpdateOrdersList
import com.woocommerce.android.util.advanceTimeAndRun
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_ERROR
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_OFFLINE
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_LOADING
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SEARCH_RESULTS
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SEARCH_RESULTS_GUEST
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
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

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class OrderListViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val orderListRepository: OrderListRepository = mock {
        on { fetchPaymentGateways() } doReturn RequestResult.SUCCESS
        on { fetchOrderStatusOptionsFromApi() } doReturn RequestResult.SUCCESS
    }
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val dispatcher: Dispatcher = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { it.arguments[0].toString() + it.arguments[1].toString() }
    }

    private val orderStatusOptions = OrderTestUtils.generateOrderStatusOptionsMappedByStatus()
    private lateinit var viewModel: OrderListViewModel
    private val listStore: ListStore = mock()
    private val pagedListWrapper: PagedListWrapper<OrderListItemUIType> = mock()
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters = mock()
    private val getWCOrderListDescriptorWithFiltersAndSearchQuery: GetWCOrderListDescriptorWithFiltersAndSearchQuery =
        mock()
    private val getSelectedOrderFiltersCount: GetSelectedOrderFiltersCount = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val barcodeScanningTracker = mock<BarcodeScanningTracker>()
    private val notificationChannelsHandler = mock<NotificationChannelsHandler>()
    private val appPrefs = mock<AppPrefsWrapper>()
    private val showTestNotification = mock<ShowTestNotification>()
    private val shouldUpdateOrdersList = mock<ShouldUpdateOrdersList>()
    private val observeOrdersListLastUpdate = mock<ObserveOrdersListLastUpdate>()
    private val orderListItemDataSource = mock<OrderListItemDataSource>()

    @Before
    fun setup() = testBlocking {
        whenever(getWCOrderListDescriptorWithFilters.invoke()).thenReturn(WCOrderListDescriptor(site = mock()))
        whenever(getWCOrderListDescriptorWithFiltersAndSearchQuery.invoke(anyString(), anyBoolean())).thenReturn(
            WCOrderListDescriptor(
                site = mock()
            )
        )
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
        whenever(orderDetailRepository.getOrderStatusOptions()).thenReturn(emptyList())

        whenever(shouldUpdateOrdersList.invoke(any())).doReturn(true)
        whenever(observeOrdersListLastUpdate.invoke(any())).doReturn(flowOf(1721598780075L))

        viewModel = createViewModel()
    }

    private fun createViewModel(
        savedState: SavedStateHandle = OrderListFragmentArgs().toSavedStateHandle()
    ) = OrderListViewModel(
        savedState = savedState,
        dispatchers = coroutinesTestRule.testDispatchers,
        orderListRepository = orderListRepository,
        orderDetailRepository = orderDetailRepository,
        listStore = listStore,
        networkStatus = networkStatus,
        dispatcher = dispatcher,
        selectedSite = selectedSite,
        resourceProvider = resourceProvider,
        getWCOrderListDescriptorWithFilters = getWCOrderListDescriptorWithFilters,
        getWCOrderListDescriptorWithFiltersAndSearchQuery = getWCOrderListDescriptorWithFiltersAndSearchQuery,
        getSelectedOrderFiltersCount = getSelectedOrderFiltersCount,
        orderListTransactionLauncher = mock(),
        analyticsTracker = analyticsTracker,
        barcodeScanningTracker = barcodeScanningTracker,
        notificationChannelsHandler = notificationChannelsHandler,
        appPrefs = appPrefs,
        showTestNotification = showTestNotification,
        dateUtils = mock(),
        shouldUpdateOrdersList = shouldUpdateOrdersList,
        observeOrdersListLastUpdate = observeOrdersListLastUpdate,
        dataSourceLazyProvider = { orderListItemDataSource },
    )

    @Test
    fun `Request to load new list fetches order status options and payment gateways if connected`() = testBlocking {
        clearInvocations(orderListRepository)
        viewModel.onSearchOpened()
        viewModel.onSearchSubmitted(ANY_SEARCH_QUERY)

        verify(viewModel.activePagedListWrapper, times(1))?.fetchFirstPage()
        verify(orderListRepository, times(1)).fetchPaymentGateways()
        verify(orderListRepository, times(1)).fetchOrderStatusOptionsFromApi()
    }

    @Test
    fun `Load orders activates list wrapper`() = testBlocking {
        doReturn(RequestResult.SUCCESS).whenever(orderListRepository).fetchPaymentGateways()
        whenever(shouldUpdateOrdersList.invoke(any())).doReturn(true)

        viewModel.loadOrders()

        assertNotNull(viewModel.ordersPagedListWrapper)
        assertNotNull(viewModel.activePagedListWrapper)
        assertEquals(viewModel.ordersPagedListWrapper, viewModel.activePagedListWrapper)

        verify(viewModel.ordersPagedListWrapper, times(1))?.invalidateData()
        // When should update list is true, then fetch the first page
        verify(viewModel.ordersPagedListWrapper, times(1))?.fetchFirstPage()
    }

    @Test
    fun `Load orders with cache doesn't fetch data`() = testBlocking {
        whenever(shouldUpdateOrdersList.invoke(any())).doReturn(false)

        viewModel.loadOrders()

        assertNotNull(viewModel.ordersPagedListWrapper)
        assertNotNull(viewModel.activePagedListWrapper)
        assertEquals(viewModel.ordersPagedListWrapper, viewModel.activePagedListWrapper)

        // When should update list is false, then DON'T fetch the first page and rely on cached data (DB)
        verify(viewModel.ordersPagedListWrapper, never())?.fetchFirstPage()
        verify(viewModel.ordersPagedListWrapper, times(1))?.invalidateData()
    }

    @Test
    fun `when search is opened, then the active list presentation is no longer exposed`() {
        val pagedList = mock<PagedList<OrderListItemUIType>>()
        whenever(pagedListWrapper.data).thenReturn(MutableLiveData(pagedList))
        viewModel.pagedListData.observeForever { }
        viewModel.loadOrders()

        viewModel.onSearchOpened()

        assertThat(viewModel.pagedListData.value).isNull()
        assertThat(viewModel.emptyViewType.value).isNull()
    }

    @Test
    fun `given an unchanged descriptor, when orders reload after search opens, then cached list is restored`() {
        val pagedList = mock<PagedList<OrderListItemUIType>>()
        whenever(pagedListWrapper.data).thenReturn(MutableLiveData(pagedList))
        viewModel.pagedListData.observeForever { }
        viewModel.loadOrders()
        viewModel.onSearchOpened()

        viewModel.loadOrders()

        assertThat(viewModel.pagedListData.value).isSameAs(pagedList)
    }

    @Test
    fun `when search is opened and orders reload, then menu search analytics is tracked exactly once`() {
        // GIVEN
        clearInvocations(analyticsTracker)

        // WHEN
        viewModel.onSearchOpened()
        viewModel.onSearchOpened()
        viewModel.loadOrders()

        // THEN
        verify(analyticsTracker, times(1)).track(AnalyticsEvent.ORDERS_LIST_MENU_SEARCH_TAPPED)
    }

    @Test
    fun `given rapid query changes, when debounce completes, then only the latest search executes`() = testBlocking {
        // GIVEN
        clearInvocations(getWCOrderListDescriptorWithFiltersAndSearchQuery, analyticsTracker)
        viewModel.onSearchOpened()
        viewModel.onSearchQueryChanged("first")
        advanceTimeAndRun(AppConstants.SEARCH_TYPING_DELAY_MS - 1)
        verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, never()).invoke(anyString(), anyBoolean())

        // WHEN
        viewModel.onSearchQueryChanged("second")
        advanceTimeAndRun(AppConstants.SEARCH_TYPING_DELAY_MS)

        // THEN
        verify(getWCOrderListDescriptorWithFiltersAndSearchQuery).invoke(
            searchQuery = "second",
            searchGuestOrders = false
        )
        verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, never()).invoke(
            searchQuery = "first",
            searchGuestOrders = false
        )
        verify(analyticsTracker).track(
            AnalyticsEvent.ORDERS_LIST_SEARCH,
            mapOf(AnalyticsTracker.KEY_SEARCH to "second")
        )
    }

    @Test
    fun `given a pending search, when orders load, then passive search cancels debounce without analytics`() =
        testBlocking {
            // GIVEN
            clearInvocations(getWCOrderListDescriptorWithFiltersAndSearchQuery, analyticsTracker)
            viewModel.onSearchOpened()
            viewModel.onSearchQueryChanged("query")

            // WHEN
            viewModel.loadOrders()
            advanceUntilIdle()

            // THEN
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, times(1)).invoke(
                searchQuery = "query",
                searchGuestOrders = false
            )
            verify(analyticsTracker, never()).track(
                eq(AnalyticsEvent.ORDERS_LIST_SEARCH),
                any<Map<String, *>>()
            )
        }

    @Test
    fun `given a short query, when submitted, then search executes immediately`() = testBlocking {
        // GIVEN
        clearInvocations(getWCOrderListDescriptorWithFiltersAndSearchQuery, analyticsTracker)
        viewModel.onSearchOpened()
        viewModel.onSearchQueryChanged("ab")
        advanceTimeAndRun(AppConstants.SEARCH_TYPING_DELAY_MS)
        verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, never()).invoke(anyString(), anyBoolean())

        // WHEN
        viewModel.onSearchSubmitted("ab")

        // THEN
        verify(getWCOrderListDescriptorWithFiltersAndSearchQuery).invoke(
            searchQuery = "ab",
            searchGuestOrders = false
        )
        verify(analyticsTracker).track(
            AnalyticsEvent.ORDERS_LIST_SEARCH,
            mapOf(AnalyticsTracker.KEY_SEARCH to "ab")
        )
    }

    @Test
    fun `given a pending search, when submitted, then debounce is cancelled and search executes once`() =
        testBlocking {
            // GIVEN
            clearInvocations(getWCOrderListDescriptorWithFiltersAndSearchQuery, analyticsTracker)
            viewModel.onSearchOpened()
            viewModel.onSearchQueryChanged("query")

            // WHEN
            viewModel.onSearchSubmitted("query")

            // THEN
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery).invoke(
                searchQuery = "query",
                searchGuestOrders = false
            )
            advanceUntilIdle()
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, times(1)).invoke(
                searchQuery = "query",
                searchGuestOrders = false
            )
            verify(analyticsTracker, times(1)).track(
                AnalyticsEvent.ORDERS_LIST_SEARCH,
                mapOf(AnalyticsTracker.KEY_SEARCH to "query")
            )
        }

    @Test
    fun `given an active search, when cleared, then normal orders reload and search stays open`() = testBlocking {
        // GIVEN
        clearInvocations(
            getWCOrderListDescriptorWithFilters,
            getWCOrderListDescriptorWithFiltersAndSearchQuery,
            analyticsTracker
        )
        viewModel.onSearchOpened()
        viewModel.onSearchQueryChanged("query")

        // WHEN
        viewModel.onSearchCleared()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.isSearching).isTrue()
        assertThat(viewModel.viewState.searchQuery).isEmpty()
        verify(getWCOrderListDescriptorWithFilters).invoke()
        verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, never()).invoke(anyString(), anyBoolean())
        verify(analyticsTracker, never()).track(
            eq(AnalyticsEvent.ORDERS_LIST_SEARCH),
            any<Map<String, *>>()
        )
    }

    @Test
    fun `given a pending search, when closed, then normal orders reload and pending search is cancelled`() =
        testBlocking {
            // GIVEN
            clearInvocations(
                getWCOrderListDescriptorWithFilters,
                getWCOrderListDescriptorWithFiltersAndSearchQuery,
                analyticsTracker
            )
            viewModel.onSearchOpened()
            viewModel.onSearchQueryChanged("query")

            // WHEN
            viewModel.onSearchClosed()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.viewState.isSearching).isFalse()
            assertThat(viewModel.viewState.searchQuery).isEmpty()
            verify(getWCOrderListDescriptorWithFilters).invoke()
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, never()).invoke(anyString(), anyBoolean())
            verify(analyticsTracker, never()).track(
                eq(AnalyticsEvent.ORDERS_LIST_SEARCH),
                any<Map<String, *>>()
            )
        }

    @Test
    fun `given a hash-prefixed query, when debounce completes, then raw state is sanitized only for search`() =
        testBlocking {
            // GIVEN
            clearInvocations(getWCOrderListDescriptorWithFiltersAndSearchQuery, analyticsTracker)
            viewModel.onSearchOpened()

            // WHEN
            viewModel.onSearchQueryChanged("#123")
            advanceTimeAndRun(AppConstants.SEARCH_TYPING_DELAY_MS)

            // THEN
            assertThat(viewModel.viewState.searchQuery).isEqualTo("#123")
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery).invoke(
                searchQuery = "123",
                searchGuestOrders = false
            )
            verify(analyticsTracker).track(
                AnalyticsEvent.ORDERS_LIST_SEARCH,
                mapOf(AnalyticsTracker.KEY_SEARCH to "#123")
            )
        }

    @Test
    fun `given a restored active search, when ViewModel is recreated, then search resumes without analytics`() =
        testBlocking {
            // GIVEN
            val savedState = OrderListFragmentArgs().toSavedStateHandle().apply {
                this[OrderListViewModel.ViewState::class.java.name] = OrderListViewModel.ViewState(
                    isSearching = true,
                    searchQuery = "restored query"
                )
            }
            whenever(selectedSite.exists()).thenReturn(true)
            clearInvocations(getWCOrderListDescriptorWithFiltersAndSearchQuery, analyticsTracker)

            // WHEN
            viewModel = createViewModel(savedState)
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.viewState.isSearching).isTrue()
            assertThat(viewModel.viewState.searchQuery).isEqualTo("restored query")
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery).invoke(
                searchQuery = "restored query",
                searchGuestOrders = false
            )
            verify(analyticsTracker, never()).track(
                eq(AnalyticsEvent.ORDERS_LIST_SEARCH),
                any<Map<String, *>>()
            )
            verify(analyticsTracker, never()).track(AnalyticsEvent.ORDERS_LIST_MENU_SEARCH_TAPPED)
        }

    @Test
    fun `given a restored active short query, when ViewModel is recreated, then presentation stays cleared`() =
        testBlocking {
            // GIVEN
            val savedState = OrderListFragmentArgs().toSavedStateHandle().apply {
                this[OrderListViewModel.ViewState::class.java.name] = OrderListViewModel.ViewState(
                    isSearching = true,
                    searchQuery = "ab"
                )
            }
            whenever(selectedSite.exists()).thenReturn(true)
            clearInvocations(
                getWCOrderListDescriptorWithFilters,
                getWCOrderListDescriptorWithFiltersAndSearchQuery,
                analyticsTracker
            )

            // WHEN
            viewModel = createViewModel(savedState)
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.viewState.isSearching).isTrue()
            assertThat(viewModel.viewState.searchQuery).isEqualTo("ab")
            assertThat(viewModel.pagedListData.value).isNull()
            assertThat(viewModel.emptyViewType.value).isNull()
            verify(getWCOrderListDescriptorWithFilters, never()).invoke()
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, never()).invoke(anyString(), anyBoolean())
            verify(analyticsTracker, never()).track(
                eq(AnalyticsEvent.ORDERS_LIST_SEARCH),
                any<Map<String, *>>()
            )
            verify(analyticsTracker, never()).track(AnalyticsEvent.ORDERS_LIST_MENU_SEARCH_TAPPED)
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

    @Test
    fun `when orders are pulled to refresh, then track the gesture and refresh the active list`() = testBlocking {
        viewModel.loadOrders()
        clearInvocations(analyticsTracker, pagedListWrapper)

        viewModel.onPullToRefresh()

        verify(analyticsTracker).track(AnalyticsEvent.ORDERS_LIST_PULLED_TO_REFRESH)
        verify(pagedListWrapper).fetchFirstPage()
    }

    /**
     * Test the logic that generates the "No orders yet" empty view for the ALL tab
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewType].
     *
     * This view gets generated when:
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isError = null
     * - viewModel.orderStatusFilter = ""
     * - viewModel.viewState.isSearching = false
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.data != null
     * - There are NO orders in the db for the active store
     */
    @Test
    fun `Display 'No orders yet' empty view when no orders for site for ALL tab`() = testBlocking {
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
     * - viewModel.viewState.isSearching = false
     * - viewModel.orderStatusFilter = ""
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.isError = TRUE
     */
    @Test
    fun `Display error empty view on fetch orders error when no cached orders`() = testBlocking {
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
     * - viewModel.viewState.isSearching = false
     * - viewModel.orderStatusFilter = ""
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Display offline empty view when offline and list is empty`() = testBlocking {
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
     * - viewModel.viewState.isSearching = true
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = false
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Display empty view for empty search result`() = testBlocking {
        viewModel.onSearchOpened()
        viewModel.onSearchSubmitted("query")
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
     * Test the logic that generates the guest-orders search empty view. It is shown instead of the
     * regular search empty view when the search query matches the localized Guest label displayed
     * on order list rows (guest orders don't store that label, so text search can't find them),
     * as long as no customer filter is active.
     */
    @Test
    fun `given search query matches the guest label, when there are no results, then show guest empty view`() = testBlocking {
        whenever(resourceProvider.getString(R.string.orderdetail_customer_name_default)).doReturn("Guest")
        viewModel.onSearchOpened()
        viewModel.onSearchSubmitted(" guest ")
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
            assertEquals(emptyView, SEARCH_RESULTS_GUEST)
        }
    }

    @Test
    fun `given a customer filter is active, when guest label search has no results, then show regular search empty view`() = testBlocking {
        whenever(resourceProvider.getString(R.string.orderdetail_customer_name_default)).doReturn("Guest")
        whenever(getWCOrderListDescriptorWithFiltersAndSearchQuery.invoke(anyString(), anyBoolean())).thenReturn(
            WCOrderListDescriptor(site = mock(), customerId = 123L)
        )
        givenActiveSearchQuery("guest")
        viewModel.onSearchSubmitted("guest")
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

    @Test
    fun `when the guest orders empty view button is clicked, then search for guest orders`() = testBlocking {
        givenActiveSearchQuery("Guest")

        viewModel.onSearchGuestOrdersClicked()

        verify(getWCOrderListDescriptorWithFiltersAndSearchQuery).invoke(
            searchQuery = "Guest",
            searchGuestOrders = true
        )
    }

    @Test
    fun `given a guest orders search, when the same query is re-submitted, then the guest filter is kept`() =
        testBlocking {
            // GIVEN
            givenActiveSearchQuery("Guest")
            viewModel.onSearchGuestOrdersClicked()

            // WHEN
            viewModel.onSearchSubmitted("Guest")

            // THEN the guest filter is kept
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, times(2)).invoke(
                searchQuery = "Guest",
                searchGuestOrders = true
            )
        }

    @Test
    fun `given a guest orders search, when a different query is submitted, then the guest filter is cleared`() =
        testBlocking {
            givenActiveSearchQuery("Guest")
            viewModel.onSearchGuestOrdersClicked()

            viewModel.onSearchSubmitted("Guests")

            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery).invoke(
                searchQuery = "Guests",
                searchGuestOrders = false
            )
            // AND the guest filter is not restored for the original query anymore
            viewModel.onSearchSubmitted("Guest")
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery, times(1)).invoke(
                searchQuery = "Guest",
                searchGuestOrders = false
            )
        }

    @Test
    fun `given a guest orders search, when the search is closed, then the guest filter is cleared`() =
        testBlocking {
            givenActiveSearchQuery("Guest")
            viewModel.onSearchGuestOrdersClicked()

            viewModel.onSearchClosed()

            viewModel.onSearchOpened()
            viewModel.onSearchSubmitted("Guest")
            verify(getWCOrderListDescriptorWithFiltersAndSearchQuery).invoke(
                searchQuery = "Guest",
                searchGuestOrders = false
            )
        }

    /**
     * Test the logic that generates the Loading empty list view for any tab of the order list
     * is successful and verify the view is emitted via [OrderListViewModel.emptyViewType].
     *
     * This view gets generated when:
     * - viewModel.viewState.isSearching = false
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = true
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Display Loading empty view for any order list tab`() = testBlocking {
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
     * - viewModel.viewState.isSearching = true
     * - pagedListWrapper.isEmpty = true
     * - pagedListWrapper.isFetchingFirstPage = true
     * - pagedListWrapper.isError = null
     */
    @Test
    fun `Does not display the Loading empty view in search mode`() = testBlocking {
        viewModel.onSearchOpened()
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
     * in the flow we use [FetchOrdersRepository] which filters out requests that duplicate requests
     * of fetching order.
     */
    @Test
    fun `Request refresh for active list when received new order notification and is in search`() = testBlocking {
        viewModel.onSearchOpened()
        viewModel.onSearchSubmitted("Joe Doe")

        // Reset as we're no interested in previous invocations in this test
        reset(viewModel.activePagedListWrapper)
        viewModel.onNotificationReceived(
            NotificationReceivedEvent(siteId = 0L, NotificationChannelType.NEW_ORDER)
        )

        verify(viewModel.activePagedListWrapper)?.fetchFirstPage()
    }

    @Test
    fun `when the order is swiped then the status is changed optimistically`() = testBlocking {
        // Given that updateOrderStatus will success
        val order = OrderTestUtils.generateOrder()
        val gesture = OrderStatusUpdateSource.SwipeToCompleteGesture(order.orderId, order.status)
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
        assertTrue(optimisticChangeEvent is ShowUndoSnackbar)

        advanceTimeBy(1_001)

        // Then when the order status changed nothing happens because it was already handled optimistically
        val resultEvent = viewModel.event.getOrAwaitValue()
        assertEquals(optimisticChangeEvent, resultEvent)
    }

    @Test
    fun `given a swiped order moves, when completion and Undo resolve, then the same order is updated`() = testBlocking {
        val originalStatus = CoreOrderStatus.PROCESSING.value
        val targetOrder = OrderListItemUIType.OrderListItemUI(
            orderId = 11L,
            orderNumber = "11",
            orderName = "First customer",
            orderTotal = "10",
            status = originalStatus,
            dateCreated = null,
            currencyCode = "USD"
        )
        val otherOrder = targetOrder.copy(orderId = 12L, orderNumber = "12")
        val displayedItems = mutableListOf<OrderListItemUIType>(targetOrder, otherOrder)
        val pagedList = mock<PagedList<OrderListItemUIType>> {
            on { iterator() } doAnswer { displayedItems.iterator() }
        }
        whenever(pagedList.snapshot()).thenReturn(pagedList)
        whenever(pagedListWrapper.data).thenReturn(MutableLiveData(pagedList))
        viewModel.pagedListData.observeForever { }
        viewModel.loadOrders()
        whenever(
            orderDetailRepository.updateOrderStatus(targetOrder.orderId, CoreOrderStatus.COMPLETED.value)
        ).thenReturn(
            flow {
                displayedItems.reverse()
                emit(WCOrderStore.UpdateOrderResult.OptimisticUpdateResult(WCOrderStore.OnOrderChanged()))
            }
        )
        whenever(orderDetailRepository.updateOrderStatus(targetOrder.orderId, originalStatus)).thenReturn(
            flow {
                displayedItems.reverse()
                emit(WCOrderStore.UpdateOrderResult.OptimisticUpdateResult(WCOrderStore.OnOrderChanged()))
            }
        )
        val events = mutableListOf<Event>()
        viewModel.event.observeForever(events::add)

        viewModel.onSwipeToComplete(targetOrder.orderId)
        advanceUntilIdle()

        assertThat(targetOrder.status).isEqualTo(CoreOrderStatus.COMPLETED.value)
        assertThat(viewModel.orderListContentRevision.value).isEqualTo(1L)
        val undoEvent = events.filterIsInstance<ShowUndoSnackbar>().single()

        undoEvent.undoAction.onClick(null)
        advanceUntilIdle()

        assertThat(targetOrder.status).isEqualTo(originalStatus)
        assertThat(viewModel.orderListContentRevision.value).isEqualTo(2L)
    }

    @Test
    fun `when the order is swiped but the change fails, then a retry message is shown`() = testBlocking {
        // Given that updateOrderStatus will fail
        val order = OrderTestUtils.generateOrder()
        val gesture = OrderStatusUpdateSource.SwipeToCompleteGesture(order.orderId, order.status)
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
        assertTrue(optimisticChangeEvent is ShowUndoSnackbar)

        advanceTimeBy(1_001)

        // Then when the order status change fails, the retry message is shown
        val resultEvent = viewModel.event.getOrAwaitValue()
        assertTrue(resultEvent is OrderListEvent.ShowRetryErrorSnack)
    }

    @Test
    fun `when fetching orders for the first time fails with timeout, then trigger a retry event`() = testBlocking {
        // given
        var lastReceivedEvent: Event? = null
        val listError = MutableLiveData(null as ListStore.ListError?)
        whenever(pagedListWrapper.listError).doReturn(listError)
        whenever(pagedListWrapper.fetchFirstPage()) doAnswer {
            listError.value = ListStore.ListError(ListStore.ListErrorType.TIMEOUT_ERROR)
        }
        viewModel.event.observeForever {
            lastReceivedEvent = it
        }

        // when
        viewModel.loadOrders()

        // then
        assertThat(lastReceivedEvent).isEqualTo(OrderListEvent.RetryLoadingOrders)
    }

    @Test
    fun `when retrying to fetch orders fails with timeout, then display the troubleshooting banner`() = testBlocking {
        // given
        var lastReceivedEvent: Event? = null
        var shouldDisplayTroubleshootingBanner: Boolean? = null
        val listError = MutableLiveData(null as ListStore.ListError?)
        whenever(pagedListWrapper.listError).doReturn(listError)
        whenever(pagedListWrapper.fetchFirstPage()) doAnswer {
            listError.value = ListStore.ListError(ListStore.ListErrorType.TIMEOUT_ERROR)
        }
        viewModel.event.observeForever {
            lastReceivedEvent = it
        }
        viewModel.viewStateLiveData.observeForever { _, new ->
            shouldDisplayTroubleshootingBanner = new.shouldDisplayTroubleshootingBanner
        }

        // when
        viewModel.loadOrders()
        assertThat(lastReceivedEvent).isEqualTo(OrderListEvent.RetryLoadingOrders)
        assertThat(shouldDisplayTroubleshootingBanner).isFalse
        viewModel.fetchOrdersAndOrderDependencies()

        // then
        assertThat(shouldDisplayTroubleshootingBanner).isTrue
    }
    // region barcode scanner

    @Test
    fun `when code scanner succeeds, then trigger proper event`() {
        val scannedStatus = CodeScannerStatus.Success(
            code = "12345",
            format = BarcodeFormat.FormatQRCode
        )
        viewModel = createViewModel()
        viewModel.handleBarcodeScannedStatus(scannedStatus)

        assertThat(viewModel.event.value).isInstanceOf(OrderListEvent.OnBarcodeScanned::class.java)
    }

    @Test
    fun `when code scanner fails, then trigger proper event`() {
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )
        viewModel = createViewModel()
        viewModel.handleBarcodeScannedStatus(scannedStatus)

        assertThat(viewModel.event.value).isInstanceOf(
            OnAddingProductViaScanningFailed::class.java
        )
    }

    @Test
    fun `when code scanner fails, then trigger event proper message`() {
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )
        viewModel = createViewModel()
        viewModel.handleBarcodeScannedStatus(scannedStatus)

        assertThat(
            (viewModel.event.value as OnAddingProductViaScanningFailed).message
        ).isEqualTo(R.string.order_list_barcode_scanning_scanning_failed)
    }

    @Test
    fun `given code scanner failure, when retry clicked, then scan restarted`() {
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )
        viewModel = createViewModel()
        viewModel.handleBarcodeScannedStatus(scannedStatus)
        (viewModel.event.value as OnAddingProductViaScanningFailed).retry.onClick(mock())

        assertThat(viewModel.event.value).isInstanceOf(OrderListEvent.OpenBarcodeScanningFragment::class.java)
    }

    @Test
    fun `when code scanner succeeds, then trigger event with proper sku`() {
        val scannedStatus = CodeScannerStatus.Success(
            code = "12345",
            format = BarcodeFormat.FormatUPCA
        )
        viewModel = createViewModel()
        viewModel.handleBarcodeScannedStatus(scannedStatus)

        assertThat(viewModel.event.value).isEqualTo(
            OrderListEvent.OnBarcodeScanned("12345", BarcodeFormat.FormatUPCA)
        )
    }

    @Test
    fun `when scan clicked, then track proper analytics event`() {
        viewModel = createViewModel()

        viewModel.onScanClicked()

        verify(analyticsTracker).track(AnalyticsEvent.ORDER_LIST_PRODUCT_BARCODE_SCANNING_TAPPED)
    }

    @Test
    fun `when scan clicked, then trigger openBarcodeScanningFragment event`() {
        viewModel = createViewModel()

        viewModel.onScanClicked()

        assertThat(viewModel.event.value).isInstanceOf(OrderListEvent.OpenBarcodeScanningFragment::class.java)
    }

    @Test
    fun `when scan success, then track proper analytics event`() {
        val scannedStatus = CodeScannerStatus.Success(
            code = "12345",
            format = BarcodeFormat.FormatUPCA
        )
        viewModel = createViewModel()

        viewModel.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackSuccess(any())
    }

    @Test
    fun `when scan success, then track proper analytics event with proper source`() {
        val scannedStatus = CodeScannerStatus.Success(
            code = "12345",
            format = BarcodeFormat.FormatUPCA
        )
        viewModel = createViewModel()

        viewModel.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackSuccess(ScanningSource.ORDER_LIST)
    }

    @Test
    fun `when scan failure, then track analytics event`() {
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )
        viewModel = createViewModel()

        viewModel.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackScanFailure(any(), any())
    }

    @Test
    fun `when scan failure, then track analytics event with proper source`() {
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )
        viewModel = createViewModel()

        viewModel.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackScanFailure(eq(ScanningSource.ORDER_LIST), any())
    }

    @Test
    fun `when scan failure, then track analytics event with proper type`() {
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.CodeScannerGooglePlayServicesVersionTooOld
        )
        viewModel = createViewModel()

        viewModel.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackScanFailure(
            any(),
            eq(CodeScanningErrorType.CodeScannerGooglePlayServicesVersionTooOld)
        )
    }

    @Test
    fun `given cha-ching sound disabled, when order list is loaded, then show a dialog`() = testBlocking {
        // given
        whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
            .thenReturn(NewOrderNotificationSoundStatus.DISABLED)
        whenever(appPrefs.chaChingSoundIssueDialogDismissed).thenReturn(false)
        whenever(pagedListWrapper.isFetchingFirstPage).doReturn(MutableLiveData(false))

        // when
        val events = viewModel.event.runAndCaptureValues {
            viewModel.loadOrders()
        }

        // then
        assertThat(events).anyMatch {
            it is Event.ShowDialog &&
                it.titleId == R.string.cha_ching_sound_issue_dialog_title &&
                it.messageId == R.string.cha_ching_sound_issue_dialog_message &&
                it.positiveButtonId == R.string.cha_ching_sound_issue_dialog_turn_on_sound &&
                it.negativeButtonId == R.string.cha_ching_sound_issue_dialog_keep_silent
        }
    }

    @Test
    fun `when cha-ching dialog is shown, then clicking turn on sound should re-create notification channel`() = testBlocking {
        // given
        whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
            .thenReturn(NewOrderNotificationSoundStatus.DISABLED)
        whenever(appPrefs.chaChingSoundIssueDialogDismissed).thenReturn(false)
        whenever(pagedListWrapper.isFetchingFirstPage).doReturn(MutableLiveData(false))

        // when
        val event = viewModel.event.runAndCaptureValues {
            viewModel.loadOrders()
        }.first { it is Event.ShowDialog } as Event.ShowDialog
        event.positiveBtnAction!!.onClick(null, 0)

        // then
        verify(notificationChannelsHandler).recreateNotificationChannel(NotificationChannelType.NEW_ORDER)
    }

    @Test
    fun `when cha-ching dialog is shown, then clicking turn keep silent should mark dialog as dismissed`() = testBlocking {
        // given
        whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
            .thenReturn(NewOrderNotificationSoundStatus.DISABLED)
        whenever(appPrefs.chaChingSoundIssueDialogDismissed).thenReturn(false)
        whenever(pagedListWrapper.isFetchingFirstPage).doReturn(MutableLiveData(false))

        // when
        val event = viewModel.event.runAndCaptureValues {
            viewModel.loadOrders()
        }.first { it is Event.ShowDialog } as Event.ShowDialog
        event.negativeBtnAction!!.onClick(null, 0)

        // then
        verify(appPrefs).chaChingSoundIssueDialogDismissed = true
    }

    @Test
    fun `given cha-ching dialog dismissed, when order list is loaded, then don't show a dialog`() = testBlocking {
        // given
        whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
            .thenReturn(NewOrderNotificationSoundStatus.DISABLED)
        whenever(appPrefs.chaChingSoundIssueDialogDismissed).thenReturn(true)
        whenever(pagedListWrapper.isFetchingFirstPage).doReturn(MutableLiveData(false))

        // when
        val events = viewModel.event.runAndCaptureValues {
            viewModel.loadOrders()
        }

        // then
        assertThat(events).noneMatch {
            it is Event.ShowDialog &&
                it.titleId == R.string.cha_ching_sound_issue_dialog_title &&
                it.messageId == R.string.cha_ching_sound_issue_dialog_message &&
                it.positiveButtonId == R.string.cha_ching_sound_issue_dialog_turn_on_sound &&
                it.negativeButtonId == R.string.cha_ching_sound_issue_dialog_keep_silent
        }
    }

    @Test
    fun `when order trash is requested, then trash order and show an undo snackbar`() = testBlocking {
        whenever(orderListRepository.trashOrder(any())).thenReturn(Result.success(Unit))
        viewModel.loadOrders()

        val undoSnackbar = viewModel.event.runAndCaptureValues {
            viewModel.trashOrder(1L)
        }.last() as ShowUndoSnackbar
        undoSnackbar.dismissAction.onDismissed(null, Snackbar.Callback.DISMISS_EVENT_TIMEOUT)

        verify(orderListRepository).trashOrder(1L)
    }

    @Test
    fun `when order trash fails, then show a snackbar`() = testBlocking {
        whenever(orderListRepository.trashOrder(any())).thenReturn(Result.failure(Exception()))
        viewModel.loadOrders()

        val undoSnackbar = viewModel.event.runAndCaptureValues {
            viewModel.trashOrder(1L)
        }.last() as ShowUndoSnackbar
        val event = viewModel.event.runAndCaptureValues {
            undoSnackbar.dismissAction.onDismissed(null, Snackbar.Callback.DISMISS_EVENT_TIMEOUT)
        }.last()

        assertThat(event).isInstanceOf(ShowErrorSnack::class.java)
    }
    //endregion

    @Test
    fun `when the search view is closed while a search is in progress, then isFetchingFirstPage is reset to false`() {
        // Trying to simulate a quick search close
        whenever(pagedListWrapper.isFetchingFirstPage).doReturn(MutableLiveData(true), MutableLiveData())

        viewModel = createViewModel()

        var isFetchingFirstPage: Boolean? = null
        viewModel.isFetchingFirstPage.observeForever {
            isFetchingFirstPage = it
        }

        viewModel.onSearchOpened()
        viewModel.onSearchSubmitted("query")
        viewModel.onSearchClosed()

        assertNotNull(isFetchingFirstPage)

        // Check that isFetchingFirstPage is reset to default value (false) on clearLiveDataSources
        assertFalse(isFetchingFirstPage)
    }

    @Test
    fun `when orders are selected, then selected IDs are the selection authority`() = testBlocking {
        selectOrders(2)

        assertThat(viewModel.isSelecting()).isTrue()
        assertThat(viewModel.selectedOrderIds.value).containsExactly(1L, 2L)
    }

    @Test
    fun `when selection is cleared, then exit selection mode`() = testBlocking {
        // First enter selection mode
        selectOrders(2)

        // Then exit
        viewModel.clearOrderSelection()

        assertThat(viewModel.isSelecting()).isFalse()
        assertThat(viewModel.selectedOrderIds.value).isEmpty()
    }

    @Test
    fun `when another order is selected, then append its ID without leaving selection mode`() = testBlocking {
        // Enter selection mode
        selectOrders(2)

        // Change count
        viewModel.onOrderLongPressed(3L)

        assertThat(viewModel.selectedOrderIds.value).containsExactly(1L, 2L, 3L)
        assertThat(viewModel.isSelecting()).isTrue()
    }

    @Test
    fun `when an order is long pressed, then its ID starts the selection`() {
        val accepted = viewModel.onOrderLongPressed(11L)

        assertThat(accepted).isTrue()
        assertThat(viewModel.selectedOrderIds.value).containsExactly(11L)
    }

    @Test
    fun `given selected orders, when their selection is toggled, then IDs remain insertion ordered`() {
        viewModel.onOrderLongPressed(11L)
        viewModel.onOrderLongPressed(12L)

        assertThat(viewModel.toggleOrderSelection(11L)).isTrue()
        assertThat(viewModel.toggleOrderSelection(13L)).isTrue()

        assertThat(viewModel.selectedOrderIds.value).containsExactly(12L, 13L)
    }

    @Test
    fun `given the selection limit, when another order is selected, then it is rejected truthfully`() {
        val events = mutableListOf<Event>()
        viewModel.event.observeForever(events::add)

        repeat(BULK_UPDATE_COUNT_LIMIT) { index ->
            assertThat(viewModel.toggleOrderSelection(index.toLong())).isTrue()
        }

        assertThat(viewModel.selectedOrderIds.value).hasSize(BULK_UPDATE_COUNT_LIMIT)
        assertThat(viewModel.toggleOrderSelection(BULK_UPDATE_COUNT_LIMIT.toLong())).isFalse()
        assertThat(viewModel.selectedOrderIds.value).hasSize(BULK_UPDATE_COUNT_LIMIT)
        assertThat(events.filterIsInstance<OrderListEvent.ShowSnackbarString>()).hasSize(1)
    }

    @Test
    fun `given selected order IDs, when the ViewModel is recreated, then selection is restored`() = testBlocking {
        val savedState = OrderListFragmentArgs().toSavedStateHandle()
        viewModel = createViewModel(savedState)
        viewModel.onOrderLongPressed(21L)
        viewModel.onOrderLongPressed(22L)
        advanceUntilIdle()

        viewModel = createViewModel(savedState)

        assertThat(viewModel.selectedOrderIds.value).containsExactly(21L, 22L)
        assertThat(viewModel.isSelecting()).isTrue()
        assertThat(viewModel.event.value).isNull()
    }

    @Test
    fun `when selection is cleared, then all selected IDs are removed`() {
        viewModel.onOrderLongPressed(11L)
        viewModel.onOrderLongPressed(12L)

        viewModel.clearOrderSelection()

        assertThat(viewModel.selectedOrderIds.value).isEmpty()
    }

    @Test
    fun `when bulk update clicked, then trigger dialog event with status options`() = testBlocking {
        // Given
        val statusOptions = listOf(
            Order.OrderStatus(CoreOrderStatus.COMPLETED.value, "Completed"),
            Order.OrderStatus(CoreOrderStatus.PROCESSING.value, "Processing")
        )
        whenever(orderDetailRepository.getOrderStatusOptions()).thenReturn(statusOptions)
        selectOrders(2)

        // When
        viewModel.onBulkUpdateStatusClicked()

        // Then
        assertThat(viewModel.event.value).isInstanceOf(OrderListEvent.ShowUpdateStatusDialog::class.java)
    }

    @Test
    fun `given offline, when bulk update status requested, then show offline error and exit selection mode`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(false)

        requestBulkUpdateFor(2)
        viewModel.onBulkOrderStatusChanged(Order.Status.Completed)

        assertThat(viewModel.event.value).isInstanceOf(Event.ShowSnackbar::class.java)
        assertThat((viewModel.event.value as Event.ShowSnackbar).message).isEqualTo(R.string.offline_error)
        assertThat(viewModel.isSelecting()).isFalse()
        assertThat(viewModel.selectedOrderIds.value).isEmpty()
    }

    @Test
    fun `when bulk update fails, then show error message and exit selection`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(orderListRepository.bulkUpdateOrderStatus(any(), any()))
            .thenReturn(BulkUpdateOrderResult.Error(Exception()))

        requestBulkUpdateFor(1)
        viewModel.onBulkOrderStatusChanged(Order.Status.Completed)

        assertThat(viewModel.event.value).isInstanceOf(Event.ShowSnackbar::class.java)
        assertThat((viewModel.event.value as Event.ShowSnackbar).message).isEqualTo(R.string.error_generic)
        assertThat(viewModel.isSelecting()).isFalse()
    }

    @Test
    fun `when bulk update results in no orders updated, then show message and exit selection mode`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(orderListRepository.bulkUpdateOrderStatus(any(), any()))
            .thenReturn(BulkUpdateOrderResult.NoOrdersUpdated)

        requestBulkUpdateFor(1)
        viewModel.onBulkOrderStatusChanged(Order.Status.Completed)

        assertThat(viewModel.event.value).isInstanceOf(Event.ShowSnackbar::class.java)
        assertThat((viewModel.event.value as Event.ShowSnackbar).message)
            .isEqualTo(R.string.orderlist_bulk_update_result_no_orders_updated)
        assertThat(viewModel.isSelecting()).isFalse()
    }

    @Test
    fun `when bulk update fails for all orders, then show message and exit selection mode`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(orderListRepository.bulkUpdateOrderStatus(any(), any()))
            .thenReturn(BulkUpdateOrderResult.AllFailed)

        requestBulkUpdateFor(1)
        viewModel.onBulkOrderStatusChanged(Order.Status.Completed)

        assertThat(viewModel.event.value).isInstanceOf(Event.ShowSnackbar::class.java)
        assertThat((viewModel.event.value as Event.ShowSnackbar).message)
            .isEqualTo(R.string.orderlist_bulk_update_result_all_failed)
        assertThat(viewModel.isSelecting()).isFalse()
    }

    @Test
    fun `when bulk update fully succeeds, then refresh and exit selection mode`() = testBlocking {
        // Given
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(orderListRepository.bulkUpdateOrderStatus(any(), any()))
            .thenReturn(BulkUpdateOrderResult.AllSuccess)
        val pagedListData = MutableLiveData<PagedList<OrderListItemUIType>>(mock())
        whenever(pagedListWrapper.data).thenReturn(pagedListData)

        // First load order to initialize orderPagedListWrapper, then enter selection mode
        viewModel.loadOrders()
        requestBulkUpdateFor(2)

        // When
        viewModel.onBulkOrderStatusChanged(Order.Status.Completed)
        // Sending a different instance of PagedList to trigger the Snackbar
        pagedListData.value = mock()

        // Then
        assertThat(viewModel.isSelecting()).isFalse()
        val expectedEvent = OrderListEvent.ShowSnackbarString(
            resourceProvider.getString(R.string.orderlist_bulk_update_status_updated)
        )
        assertThat(viewModel.event.value).isEqualTo(expectedEvent)

        // Invoked once during loadOrders() and once during onBulkOrderStatusChanged()
        verify(viewModel.ordersPagedListWrapper, times(2))?.fetchFirstPage()
    }

    @Test
    fun `when bulk update partially succeeds, then refresh and exit selection mode`() = testBlocking {
        // Given
        val successCount = 3
        val failureCount = 2
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(orderListRepository.bulkUpdateOrderStatus(any(), any()))
            .thenReturn(BulkUpdateOrderResult.PartialSuccess(successCount, failureCount))

        // First load order to initialize orderPagedListWrapper, then enter selection mode
        viewModel.loadOrders()
        requestBulkUpdateFor(5)

        // When
        viewModel.onBulkOrderStatusChanged(Order.Status.Completed)

        // Then
        assertThat(viewModel.isSelecting()).isFalse()

        // Invoked once during loadOrders() and once during onBulkOrderStatusChanged()
        verify(viewModel.ordersPagedListWrapper, times(2))?.fetchFirstPage()
    }

    @Test
    fun `when selection reaches limit, then show error message`() {
        // when
        selectOrders(BULK_UPDATE_COUNT_LIMIT)

        // then
        assertThat(viewModel.selectedOrderIds.value).hasSize(BULK_UPDATE_COUNT_LIMIT)

        viewModel.event.getOrAwaitValue().let { event ->
            assertTrue(event is OrderListEvent.ShowSnackbarString)
            assertEquals(
                event.message,
                resourceProvider.getString(
                    R.string.orderlist_bulk_update_maximum_reached,
                    BULK_UPDATE_COUNT_LIMIT
                )
            )
        }
    }

    private fun givenActiveSearchQuery(query: String) {
        viewModel.onSearchOpened()
        viewModel.onSearchQueryChanged(query)
    }

    private fun selectOrders(count: Int) {
        repeat(count) { index ->
            viewModel.onOrderLongPressed(index.toLong() + 1L)
        }
    }

    private fun requestBulkUpdateFor(orderCount: Int) {
        selectOrders(orderCount)
        viewModel.onBulkUpdateStatusClicked()
    }

    private companion object {
        const val ANY_SEARCH_QUERY = "search query"
    }
}
