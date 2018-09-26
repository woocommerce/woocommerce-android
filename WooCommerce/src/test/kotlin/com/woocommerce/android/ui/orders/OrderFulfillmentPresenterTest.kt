package com.woocommerce.android.ui.orders

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore

class OrderFulfillmentPresenterTest {
    private val view: OrderFulfillmentContract.View = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val uiMessageResolver: UIMessageResolver = mock()
    private val networkStatus: NetworkStatus = mock()

    private val order = OrderTestUtils.generateOrder()
    private lateinit var presenter: OrderFulfillmentPresenter

    @Before
    fun setup() {
        presenter = spy(OrderFulfillmentPresenter(orderStore, uiMessageResolver, networkStatus))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Displays order detail view correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(order.getIdentifier())
        verify(view).showOrderDetail(order)
    }

    @Test
    fun `Mark Order Complete - Processes fulfill order request correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        presenter.markOrderComplete()
        verify(view).toggleCompleteButton(isEnabled = false)
        verify(view).fulfillOrder()
    }

    @Test
    fun `Show offline message on request to mark order complete if not connected`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()
        presenter.markOrderComplete()
        verify(view, times(0)).toggleCompleteButton(any())
        verify(view, times(0)).fulfillOrder()
        verify(uiMessageResolver, times(1)).showOfflineSnack()
    }
}
