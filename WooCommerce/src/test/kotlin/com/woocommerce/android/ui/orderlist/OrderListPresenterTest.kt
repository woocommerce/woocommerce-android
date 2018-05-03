package com.woocommerce.android.ui.orderlist

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.generateWCOrderModels
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderListContract
import com.woocommerce.android.ui.orders.OrderListPresenter
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
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

    private val orders = generateWCOrderModels()
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
        presenter.loadOrders()
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(orders).whenever(orderStore).getOrdersForSite(any())
        presenter.onOrderChanged(OnOrderChanged(orders.size))
        verify(orderListView).showOrders(orders)
    }

    @Test
    fun `Displays the no orders list view correctly`() {
        // Presenter should dispatch FETCH_ORDERS on startup
        presenter.takeView(orderListView)
        presenter.loadOrders()
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(noOrders).whenever(orderStore).getOrdersForSite(any())
        presenter.onOrderChanged(OnOrderChanged(0))
        verify(orderListView).showNoOrders()
    }
}
