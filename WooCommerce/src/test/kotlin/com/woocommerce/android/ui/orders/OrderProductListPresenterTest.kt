package com.woocommerce.android.ui.orders

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.tools.SelectedSite
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore

class OrderProductListPresenterTest {
    private val view: OrderProductListContract.View = mock()
    private val orderStore: WCOrderStore = mock()
    private val refundStore: WCRefundStore = mock()
    private val selectedSite: SelectedSite = mock()

    private val order = OrderTestUtils.generateOrder()
    private lateinit var presenter: OrderProductListPresenter

    @Before
    fun setup() {
        presenter = spy(OrderProductListPresenter(orderStore, refundStore, selectedSite))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
    }

    @Test
    fun `Displays the product list view correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail("1-2-3")
        verify(view).showOrderProducts(any(), any())
    }
}
