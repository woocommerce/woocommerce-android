package com.woocommerce.android.ui.orders

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.tools.SelectedSite
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged

class OrderListPresenterTest {
    private val orderListView: OrderListContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()

    private val orders = OrderTestUtils.generateOrders()
    private val noOrders = emptyList<WCOrderModel>()
    private lateinit var presenter: OrderListPresenter

    @Before
    fun setup() {
        presenter = spy(OrderListPresenter(dispatcher, orderStore, selectedSite))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
    }

    @Test
    fun `Displays the orders list view correctly`() {
        // Presenter should dispatch FETCH_ORDERS on startup
        presenter.takeView(orderListView)
        presenter.loadOrders(true)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(orders).whenever(orderStore).getOrdersForSite(any())
        presenter.onOrderChanged(OnOrderChanged(orders.size).apply { causeOfChange = FETCH_ORDERS })
        verify(orderListView).showOrders(orders, true)
    }

    @Test
    fun `Displays the no orders list view correctly`() {
        // Presenter should dispatch FETCH_ORDERS on startup
        presenter.takeView(orderListView)
        presenter.loadOrders(true)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(noOrders).whenever(orderStore).getOrdersForSite(any())
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_ORDERS })
        verify(orderListView).showNoOrders()
    }

    @Test
    fun `Fetches orders from DB when forceRefresh is false`() {
        presenter.takeView(orderListView)
        presenter.loadOrders(false)
        verify(presenter).fetchAndLoadOrdersFromDb(false)
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `Opens order detail view correctly`() {
        presenter.takeView(orderListView)
        val orderModel = WCOrderModel()
        presenter.openOrderDetail(orderModel)
        verify(orderListView).openOrderDetail(orderModel)
    }

    @Test
    fun `Refreshes fragment state when order status updated`() {
        presenter.takeView(orderListView)

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = UPDATE_ORDER_STATUS })
        verify(orderListView, times(1)).refreshFragmentState()
    }
}
