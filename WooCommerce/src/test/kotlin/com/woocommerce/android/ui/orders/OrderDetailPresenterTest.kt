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
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged

class OrderDetailPresenterTest {
    private val orderDetailView: OrderDetailContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()

    private val order = OrderTestUtils.generateOrder()
    private val orderIdentifier = order.getIdentifier()
    private val orderNotes = OrderTestUtils.generateOrderNotes(10, 2, 1)
    private lateinit var presenter: OrderDetailPresenter

    @Before
    fun setup() {
        presenter = spy(OrderDetailPresenter(dispatcher, orderStore, selectedSite))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
    }

    @Test
    fun `Displays the order detail view correctly`() {
        presenter.takeView(orderDetailView)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier)
        verify(orderDetailView).showOrderDetail(any(), any())
    }

    @Test
    fun `Displays the order notes view correctly`() {
        // Presenter should dispatch FETCH_ORDER_NOTES once order detail is fetched
        // from the order store
        presenter.takeView(orderDetailView)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrderNotesPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(orderNotes).whenever(orderStore).getOrderNotesForOrder(any())
        presenter.onOrderChanged(OnOrderChanged(10).apply { causeOfChange = FETCH_ORDER_NOTES })
        verify(orderDetailView).updateOrderNotes(orderNotes)
    }
}
