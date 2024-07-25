package com.woocommerce.android.ui.orders

import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.FeatureFeedbackSettings
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
import com.woocommerce.android.ui.orders.filters.domain.ShouldShowCreateTestOrderScreen
import com.woocommerce.android.ui.orders.list.FetchOrdersRepository
import com.woocommerce.android.ui.orders.list.ObserveOrdersListLastUpdate
import com.woocommerce.android.ui.orders.list.OrderListFragmentArgs
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.OnAddingProductViaScanningFailed
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.ShouldUpdateOrdersList
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
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_CREATE_TEST_ORDER
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_LOADING
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SEARCH_RESULTS
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
        onBlocking { fetchPaymentGateways() } doReturn RequestResult.SUCCESS
        onBlocking { fetchOrderStatusOptionsFromApi() } doReturn RequestResult.SUCCESS
    }
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { it.arguments[0].toString() + it.arguments[1].toString() }
    }

    private val orderStatusOptions = OrderTestUtils.generateOrderStatusOptionsMappedByStatus()
    private lateinit var viewModel: OrderListViewModel
    private val listStore: ListStore = mock()
    private val pagedListWrapper: PagedListWrapper<OrderListItemUIType> = mock()
    private val orderFetcher: FetchOrdersRepository = mock()
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters = mock()
    private val getWCOrderListDescriptorWithFiltersAndSearchQuery: GetWCOrderListDescriptorWithFiltersAndSearchQuery =
        mock()
    private val getSelectedOrderFiltersCount: GetSelectedOrderFiltersCount = mock()
    private val shouldShowCreateTestOrderScreen: ShouldShowCreateTestOrderScreen = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val feedbackPrefs = mock<FeedbackPrefs>()
    private val barcodeScanningTracker = mock<BarcodeScanningTracker>()
    private val notificationChannelsHandler = mock<NotificationChannelsHandler>()
    private val appPrefs = mock<AppPrefsWrapper>()
    private val showTestNotification = mock<ShowTestNotification>()
    private val shouldUpdateOrdersList = mock<ShouldUpdateOrdersList>()
    private val observeOrdersListLastUpdate = mock<ObserveOrdersListLastUpdate>()

    @Before
    fun setup() = testBlocking {
        whenever(getWCOrderListDescriptorWithFilters.invoke()).thenReturn(WCOrderListDescriptor(site = mock()))
        whenever(getWCOrderListDescriptorWithFiltersAndSearchQuery.invoke(anyString())).thenReturn(
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

        whenever(shouldUpdateOrdersList.invoke(any())).doReturn(true)
        whenever(observeOrdersListLastUpdate.invoke(any())).doReturn(flowOf(1721598780075L))

        viewModel = createViewModel()
    }

    private fun createViewModel(mode: OrderListViewModel.Mode = OrderListViewModel.Mode.STANDARD) = OrderListViewModel(
        savedState = OrderListFragmentArgs(mode = mode).toSavedStateHandle(),
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
        getWCOrderListDescriptorWithFiltersAndSearchQuery = getWCOrderListDescriptorWithFiltersAndSearchQuery,
        getSelectedOrderFiltersCount = getSelectedOrderFiltersCount,
        orderListTransactionLauncher = mock(),
        shouldShowCreateTestOrderScreen = shouldShowCreateTestOrderScreen,
        analyticsTracker = analyticsTracker,
        feedbackPrefs = feedbackPrefs,
        barcodeScanningTracker = barcodeScanningTracker,
        notificationChannelsHandler = notificationChannelsHandler,
        appPrefs = appPrefs,
        showTestNotification = showTestNotification,
        dateUtils = mock(),
        shouldUpdateOrdersList = shouldUpdateOrdersList,
        observeOrdersListLastUpdate = observeOrdersListLastUpdate
    )

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
        whenever(shouldShowCreateTestOrderScreen()).doReturn(false)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        advanceUntilIdle()

        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, ORDER_LIST)
        }
    }

    @Test
    fun `Display 'Try test order' empty view when shouldShowCreateTestOrderScreen is true`() = testBlocking {
        viewModel.isSearching = false
        whenever(pagedListWrapper.data.value).doReturn(mock())
        whenever(pagedListWrapper.isEmpty.value).doReturn(true)
        whenever(pagedListWrapper.isFetchingFirstPage.value).doReturn(false)
        whenever(shouldShowCreateTestOrderScreen()).doReturn(true)

        viewModel.createAndPostEmptyViewType(pagedListWrapper)
        advanceUntilIdle()

        viewModel.emptyViewType.observeForTesting {
            // Verify
            val emptyView = viewModel.emptyViewType.value
            assertNotNull(emptyView)
            assertEquals(emptyView, ORDER_LIST_CREATE_TEST_ORDER)
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
     * in the flow we use [FetchOrdersRepository] which filters out requests that duplicate requests
     * of fetching order.
     */
    @Test
    fun `Request refresh for active list when received new order notification and is in search`() = testBlocking {
        viewModel.isSearching = true

        viewModel.submitSearchOrFilter(searchQuery = "Joe Doe")

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
        assertTrue(optimisticChangeEvent is Event.ShowUndoSnackbar)

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
        assertTrue(optimisticChangeEvent is Event.ShowUndoSnackbar)

        advanceTimeBy(1_001)

        // Then when the order status change fails, the retry message is shown
        val resultEvent = viewModel.event.getOrAwaitValue()
        assertTrue(resultEvent is OrderListEvent.ShowRetryErrorSnack)
    }

    @Test
    fun `when onDismissOrderCreationSimplePaymentsFeedback called, then FEATURE_FEEDBACK_BANNER tracked`() =
        testBlocking {
            // when
            viewModel.onDismissOrderCreationSimplePaymentsFeedback()

            // then
            verify(analyticsTracker).track(
                AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
                mapOf(
                    AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FEEDBACK,
                    AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
                )
            )
        }

    @Test
    fun `when onDismissOrderCreationSimplePaymentsFeedback called, then order banner visibility changed`() =
        testBlocking {
            // given
            val featureFeedbackSettings = mock<FeatureFeedbackSettings> {
                on { feedbackState }.thenReturn(FeatureFeedbackSettings.FeedbackState.DISMISSED)
            }
            whenever(
                feedbackPrefs.getFeatureFeedbackSettings(
                    FeatureFeedbackSettings.Feature.SIMPLE_PAYMENTS_AND_ORDER_CREATION
                )
            ).thenReturn(featureFeedbackSettings)

            // when
            viewModel.onDismissOrderCreationSimplePaymentsFeedback()

            // then
            assertThat(viewModel.viewState.isSimplePaymentsAndOrderCreationFeedbackVisible).isEqualTo(false)
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

    @Test
    fun `given start order creation mode, when view model created, then OpenOrderCreationWithSimplePaymentsMigration emitted`() =
        testBlocking {
            // GIVEN
            val mode = OrderListViewModel.Mode.START_ORDER_CREATION_WITH_SIMPLE_PAYMENTS_MIGRATION

            // WHEN
            viewModel = createViewModel(mode = mode)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(OrderListEvent.OpenOrderCreationWithSimplePaymentsMigration)
        }

    @Test
    fun `given standard mode, when view model created, then OpenOrderCreationWithSimplePaymentsMigration is not emitted`() =
        testBlocking {
            // GIVEN
            val mode = OrderListViewModel.Mode.STANDARD

            // WHEN
            viewModel = createViewModel(mode = mode)

            // THEN
            assertThat(viewModel.event.value).isNull()
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

    private companion object {
        const val ANY_SEARCH_QUERY = "search query"
    }
}
