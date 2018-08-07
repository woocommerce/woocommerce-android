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
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload

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
        presenter.loadOrderDetail(orderIdentifier, false)
        verify(orderDetailView).showOrderDetail(any())
    }

    @Test
    fun `Displays the order notes view correctly`() {
        // Presenter should dispatch FETCH_ORDER_NOTES once order detail is fetched
        // from the order store
        presenter.takeView(orderDetailView)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrderNotesPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(orderNotes).whenever(orderStore).getOrderNotesForOrder(any())
        presenter.onOrderChanged(OnOrderChanged(10).apply { causeOfChange = FETCH_ORDER_NOTES })
        verify(orderDetailView).updateOrderNotes(orderNotes)
    }

    @Test
    fun `Display error message on fetch order notes error`() {
        presenter.takeView(orderDetailView)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrderNotesPayload>>())

        // OnOrderChanged callback from FluxC with error should trigger error message
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = FETCH_ORDER_NOTES
            error = OrderError()
        })
        verify(orderDetailView, times(1)).showNotesErrorSnack()
    }

    @Test
    fun `Mark order complete - Displays undo snackbar correctly`() {
        presenter.takeView(orderDetailView)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, true)

        verify(orderDetailView, times(1)).showUndoOrderCompleteSnackbar()
    }

    @Test
    fun `Mark order complete - Processes success correctly`() {
        doReturn(order).whenever(presenter).orderModel
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        // Presenter should dispatch FETCH_ORDER_NOTES once order detail is fetched
        // from the order store
        presenter.takeView(orderDetailView)
        presenter.doMarkOrderComplete()
        verify(dispatcher, times(1)).dispatch(any<Action<UpdateOrderStatusPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI Update
        presenter.onOrderChanged(OnOrderChanged(1).apply { causeOfChange = UPDATE_ORDER_STATUS })
        verify(orderDetailView, times(1)).markOrderCompleteSuccess()
    }

    @Test
    fun `Display error message on mark order complete error`() {
        doReturn(order).whenever(presenter).orderModel
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        // Presenter should dispatch FETCH_ORDER_NOTES once order detail is fetched
        // from the order store
        presenter.takeView(orderDetailView)
        presenter.doMarkOrderComplete()
        verify(dispatcher, times(1)).dispatch(any<Action<UpdateOrderStatusPayload>>())

        // OnOrderChanged callback from FluxC with error should trigger error message
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = UPDATE_ORDER_STATUS
            error = OrderError()
        })
        verify(orderDetailView, times(1)).showCompleteOrderError()
    }

    @Test
    fun `Mark order complete - Reverts status after failure correctly`() {
        doReturn(order).whenever(presenter).orderModel
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        // Presenter should dispatch FETCH_ORDER_NOTES once order detail is fetched
        // from the order store
        presenter.takeView(orderDetailView)
        presenter.doMarkOrderComplete()
        verify(dispatcher, times(1)).dispatch(any<Action<UpdateOrderStatusPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI Update
        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = UPDATE_ORDER_STATUS
            error = OrderError(message = "Error")
        })
        verify(orderDetailView).markOrderCompleteFailed()
    }

    @Test
    fun `Add an order note - Displays add note snackbar correctly`() {
        doReturn(order).whenever(presenter).orderModel
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())

        presenter.takeView(orderDetailView)
        presenter.pushOrderNote(noteText = "Test order note #1", isCustomerNote = false)
        verify(dispatcher, times(1)).dispatch(any<Action<PostOrderNotePayload>>())

        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = POST_ORDER_NOTE
            error = OrderError()
        })
        verify(orderDetailView, times(1)).showAddOrderNoteSnack()
    }

    @Test
    fun `Display error message on add order note error`() {
        doReturn(order).whenever(presenter).orderModel
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())

        presenter.takeView(orderDetailView)
        presenter.pushOrderNote(noteText = "Test order note #2", isCustomerNote = false)
        verify(dispatcher, times(1)).dispatch(any<Action<PostOrderNotePayload>>())

        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = POST_ORDER_NOTE
            error = OrderError()
        })
        verify(orderDetailView, times(1)).showAddOrderNoteErrorSnack()
    }
}
