package com.woocommerce.android.ui.orders

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.tools.SelectedSite
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload

class OrderFulfillmentPresenterTest {
    private val view: OrderFulfillmentContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()

    private val order = OrderTestUtils.generateOrder()
    private lateinit var presenter: OrderFulfillmentPresenter

    @Before
    fun setup() {
        presenter = spy(OrderFulfillmentPresenter(dispatcher, orderStore, selectedSite))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
    }

    @Test
    fun `Displays order detail view correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(order.getIdentifier())
        verify(view).showOrderDetail(order)
    }

    @Test
    fun `Mark Order Complete - Displays no network error correctly`() {
        doReturn(false).whenever(view).isNetworkConnected()
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        presenter.markOrderComplete()
        verify(view).showNetworkConnectivityError()
    }

    @Test
    fun `Mark Order Complete - shows order fulfilled correctly`() {
        doReturn(true).whenever(view).isNetworkConnected()
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        presenter.markOrderComplete()
        verify(view).toggleCompleteButton(isEnabled = false)
        verify(dispatcher, times(1)).dispatch(any<Action<UpdateOrderStatusPayload>>())

        presenter.onOrderChanged(OnOrderChanged(1).apply { causeOfChange = UPDATE_ORDER_STATUS })
        verify(view).orderFulfilled()
    }

    @Test
    fun `Enable mark order complete button on error`() {
        doReturn(true).whenever(view).isNetworkConnected()
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        presenter.markOrderComplete()
        verify(view).toggleCompleteButton(isEnabled = false)
        verify(dispatcher, times(1)).dispatch(any<Action<UpdateOrderStatusPayload>>())
        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = UPDATE_ORDER_STATUS
            error = OrderError(message = "error")
        })
        verify(view).toggleCompleteButton(isEnabled = true)
    }
}
