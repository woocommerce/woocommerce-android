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
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload

class AddOrderNotePresenterTest {
    private val addOrderNoteView: AddOrderNoteContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()

    private val order = OrderTestUtils.generateOrder()
    private lateinit var presenter: AddOrderNotePresenter

    @Before
    fun setup() {
        presenter = spy(
                AddOrderNotePresenter(
                        dispatcher,
                        orderStore,
                        selectedSite,
                        networkStatus
                )
        )
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Add an order note - Displays add note snackbar correctly`() {
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())

        presenter.takeView(addOrderNoteView)
        presenter.pushOrderNote(
                orderId = order.getIdentifier(),
                noteText = "Test order note #1",
                isCustomerNote = false
        )
        verify(dispatcher, times(1)).dispatch(any<Action<PostOrderNotePayload>>())

        presenter.onOrderChanged(OnOrderChanged(1).apply { causeOfChange = POST_ORDER_NOTE })
        verify(addOrderNoteView, times(1)).showAddOrderNoteSnack()
    }

    @Test
    fun `Add an order note - Displays offline snackbar correctly`() {
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        doReturn(false).whenever(networkStatus).isConnected()

        presenter.takeView(addOrderNoteView)
        presenter.pushOrderNote(
                orderId = order.getIdentifier(),
                noteText = "Test order note #1",
                isCustomerNote = false
        )
        verify(addOrderNoteView, times(1)).showOfflineSnack()
    }
}
