package com.woocommerce.android.ui.orders

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError

class OrderListPresenterTest {
    private val orderListView: OrderListContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()

    private val orders = OrderTestUtils.generateOrders()
    private val noOrders = emptyList<WCOrderModel>()
    private lateinit var presenter: OrderListPresenter

    @Before
    fun setup() {
        presenter = spy(OrderListPresenter(dispatcher, orderStore, selectedSite, networkStatus))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Displays the orders list view correctly`() {
        presenter.takeView(orderListView)
        presenter.loadOrders(forceRefresh = true)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(orders).whenever(orderStore).getOrdersForSite(any())
        presenter.onOrderChanged(OnOrderChanged(orders.size).apply { causeOfChange = FETCH_ORDERS })
        verify(orderListView).showOrders(orders, isFreshData = true)
    }

    @Test
    fun `Passes applied order status filter to view correctly`() {
        val orderStatusFilter = "processing"
        presenter.takeView(orderListView)
        presenter.loadOrders(orderStatusFilter, forceRefresh = true)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(orders).whenever(orderStore).getOrdersForSite(any(), any())
        presenter.onOrderChanged(OnOrderChanged(orders.size, orderStatusFilter).apply { causeOfChange = FETCH_ORDERS })
        verify(orderListView).showOrders(orders, orderStatusFilter, isFreshData = true)
    }

    @Test
    fun `Displays loading skeleton then no orders list view correctly on first run`() {
        presenter.takeView(orderListView)
        presenter.loadOrders(forceRefresh = true, isFirstRun = true)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(noOrders).whenever(orderStore).getOrdersForSite(any())
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_ORDERS })
        verify(orderListView, times(1)).showEmptyView(true)
        verify(orderListView, times(2)).showLoading(true)
    }

    @Test
    fun `Displays loading indicator then orders when cached orders present`() {
        presenter.takeView(orderListView)
        doReturn(orders).whenever(orderStore).getOrdersForSite(any())
        presenter.loadOrders(forceRefresh = true, isFirstRun = false)

        // This is called twice, once for fetching orders, then again for shipment tracking
        // the payload type in `any<Action<FetchOrdersPayload>>` does not actually get evaluated.
        // The only thing this confirms is the type of `Action`
        verify(dispatcher, times(2)).dispatch(any<Action<FetchOrdersPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_ORDERS })
        verify(orderListView, atLeastOnce()).showEmptyView(false)
        verify(orderListView, never()).showEmptyView(true)
        verify(orderListView, times(1)).showLoading(true)
    }

    @Test
    fun `Fetches orders from DB when forceRefresh is false`() {
        presenter.takeView(orderListView)
        presenter.loadOrders(forceRefresh = false)
        verify(presenter).fetchAndLoadOrdersFromDb(isForceRefresh = false)
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `Fetches orders from DB with filter when forceRefresh is false`() {
        val orderFilter = "processing"
        presenter.takeView(orderListView)
        presenter.loadOrders(orderFilter, forceRefresh = false)
        verify(presenter).fetchAndLoadOrdersFromDb(orderFilter, isForceRefresh = false)
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `Opens order detail view correctly`() {
        presenter.takeView(orderListView)
        val orderModel = WCOrderModel()
        presenter.openOrderDetail(orderModel)
        verify(orderListView).showOrderDetail(orderModel)
    }

    @Test
    fun `Refreshes fragment state when order status updated`() {
        presenter.takeView(orderListView)

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = UPDATE_ORDER_STATUS })
        verify(orderListView, times(1)).refreshFragmentState()
    }

    @Test
    fun `Displays error message on fetch orders error`() {
        presenter.takeView(orderListView)
        presenter.loadOrders(forceRefresh = true)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersPayload>>())

        // OnOrderChanged callback from FluxC with error should trigger error message
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = FETCH_ORDERS
            error = OrderError()
        })
        verify(orderListView, times(1)).showLoadOrdersError()
    }

    @Test
    fun `Refreshes order on network connected event`() {
        presenter.takeView(orderListView)
        doReturn(true).whenever(orderListView).isRefreshPending

        // mock a network status change
        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(orderListView, times(1)).refreshFragmentState()
    }

    @Test
    fun `Do not refresh orders on network connected if a force refresh is not pending`() {
        presenter.takeView(orderListView)
        doReturn(false).whenever(orderListView).isRefreshPending

        // mock a network status change
        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(orderListView, times(0)).refreshFragmentState()
    }

    @Test
    fun `Do nothing on network disconnected event`() {
        presenter.takeView(orderListView)

        // mock a network status change
        presenter.onEventMainThread(ConnectionChangeEvent(false))
        verify(orderListView, times(0)).refreshFragmentState()
    }

    @Test
    fun `Load cached orders if not connected to network`() {
        presenter.takeView(orderListView)
        doReturn(false).whenever(networkStatus).isConnected()

        // mock a network status change
        presenter.loadOrders(null, true)
        verify(presenter, times(1)).fetchAndLoadOrdersFromDb(null, false)
    }

    @Test
    fun `Do not fetch more orders if not connected to network`() {
        presenter.takeView(orderListView)
        doReturn(false).whenever(networkStatus).isConnected()

        // mock a network status change
        presenter.loadMoreOrders(null)
        verify(presenter, never()).fetchAndLoadOrdersFromDb(null, false)
    }

    @Test
    fun `Show and hide order list skeleton correctly`() {
        presenter.takeView(orderListView)
        presenter.loadOrders("processing", forceRefresh = true)
        verify(orderListView, times(1)).showLoading(true)

        presenter.onOrderChanged(OnOrderChanged(orders.size).apply { causeOfChange = FETCH_ORDERS })
        verify(orderListView, times(1)).showLoading(false)
    }

    @Test
    fun `Load shipment provider lists only if orders list is loaded`() {
        // load shipment tracking only if order list is not empty
        presenter.takeView(orderListView)
        presenter.loadOrders(forceRefresh = true)

        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersPayload>>())

        doReturn(orders).whenever(orderStore).getOrdersForSite(any())
        presenter.onOrderChanged(OnOrderChanged(orders.size).apply { causeOfChange = FETCH_ORDERS })
        verify(orderListView).showOrders(orders, isFreshData = true)

        verify(presenter).loadShipmentTrackingProviders(orders[0])
    }

    @Test
    fun `Do not load shipment provider lists if already loaded when network connected`() {
        // do not load shipment tracking provider list only if already fetched
        presenter.takeView(orderListView)
        doReturn(orders).whenever(orderStore).getOrdersForSite(any())
        doReturn(true).whenever(presenter).isShipmentTrackingProviderFetched

        presenter.loadOrders(forceRefresh = false)
        verify(presenter).loadShipmentTrackingProviders(orders[0])
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `Do not load shipment provider list if not already loaded but network not connected`() {
        // Do not load shipment provider lists if not loaded and network not connected
        presenter.takeView(orderListView)
        doReturn(false).whenever(networkStatus).isConnected()
        doReturn(orders).whenever(orderStore).getOrdersForSite(any())

        presenter.loadOrders(forceRefresh = false)
        verify(presenter).loadShipmentTrackingProviders(orders[0])
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `Load shipment provider list if not already loaded but network is connected - success`() {
        // load shipment tracking provider list only if not already fetched & network is connected - success
        presenter.takeView(orderListView)
        doReturn(orders).whenever(orderStore).getOrdersForSite(any())
        presenter.loadOrders(forceRefresh = false)
        verify(orderListView).showOrders(orders, isFreshData = false)

        verify(presenter).loadShipmentTrackingProviders(orders[0])
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrderShipmentProvidersPayload>>())
        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(1))
        assertTrue(presenter.isShipmentTrackingProviderFetched)
    }

    @Test
    fun `Load shipment provider list if not already loaded but network is connected - failure`() {
        // load shipment tracking provider list only if not already fetched & network is connected - failure
        presenter.takeView(orderListView)
        doReturn(orders).whenever(orderStore).getOrdersForSite(any())
        presenter.loadOrders(forceRefresh = false)
        verify(orderListView).showOrders(orders, isFreshData = false)

        verify(presenter).loadShipmentTrackingProviders(orders[0])
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrderShipmentProvidersPayload>>())
        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(1).apply {
            error = OrderError()
        })
        assertFalse(presenter.isShipmentTrackingProviderFetched)
    }
}
