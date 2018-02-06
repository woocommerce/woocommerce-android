package com.woocommerce.android.ui.orderlist

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.generateWCOrderModels
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore

class OrderListPresenterTest {
    private val orderListView: OrderListContract.View = mock()
    private val orderStore: WCOrderStore = mock()

    private val orders = generateWCOrderModels()
    private val noOrders = ArrayList<WCOrderModel>()
    private lateinit var presenter: OrderListPresenter

    @Before
    fun setup() {
        presenter = spy(OrderListPresenter(orderStore))
    }

    @Test
    fun `Displays the orders list view correctly`() {
        doReturn(orders).whenever(orderStore).getNewOrders()
        presenter.takeView(orderListView)
        verify(orderListView).showOrders(orders)
    }

    @Test
    fun `Displays the no orders list view correctly`() {
        doReturn(noOrders).whenever(orderStore).getNewOrders()
        presenter = spy(OrderListPresenter(orderStore))
        presenter.takeView(orderListView)
        verify(orderListView).showNoOrders()
    }
}
