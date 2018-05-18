package com.woocommerce.android.ui.orders

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.tools.SelectedSite
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore

class OrderProductListPresenterTest {
    private val view: OrderProductListContract.View = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()

    private val order = OrderTestUtils.generateOrder()
    private lateinit var presenter: OrderProductListPresenter

    @Before
    fun setup() {
        presenter = spy(OrderProductListPresenter(orderStore))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
    }

    @Test
    fun `Displays the product list view correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail("1-2-3")
        verify(view).showOrderProducts(any())
    }
}
