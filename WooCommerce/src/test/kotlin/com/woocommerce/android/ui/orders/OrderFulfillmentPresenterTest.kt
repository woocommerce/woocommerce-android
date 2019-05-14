package com.woocommerce.android.ui.orders

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError

class OrderFulfillmentPresenterTest {
    private val view: OrderFulfillmentContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val uiMessageResolver: UIMessageResolver = mock()
    private val networkStatus: NetworkStatus = mock()

    private val order = OrderTestUtils.generateOrder()
    private lateinit var presenter: OrderFulfillmentPresenter

    @Before
    fun setup() {
        presenter = spy(OrderFulfillmentPresenter(
                dispatcher, orderStore, selectedSite, uiMessageResolver, networkStatus
        ))
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

    @Test
    fun `Display order shipment tracking card correctly`() {
        val orderTrackingList = OrderTestUtils.generateOrderShipmentTrackings(3, order.id)

        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        presenter.loadOrderDetail(order.getIdentifier())

        // fetch order shipment trackings
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
        verify(presenter, times(1)).requestShipmentTrackingsFromApi(any())
        verify(dispatcher, times(1)).dispatch(any<Action<*>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(orderTrackingList).whenever(orderStore).getShipmentTrackingsForOrder(any())
        presenter.onOrderChanged(OnOrderChanged(3).apply { causeOfChange = FETCH_ORDER_SHIPMENT_TRACKINGS })

        // verify that shipment tracking card is displayed
        verify(view).showOrderShipmentTrackings(orderTrackingList)
    }

    @Test
    fun `Hide order shipment tracking card on fetch order tracking error`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        presenter.loadOrderDetail(order.getIdentifier())

        // fetch order shipment trackings
        verify(dispatcher, times(1)).dispatch(any<Action<*>>())

        // OnOrderChanged callback from FluxC with error should trigger error message
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = FETCH_ORDER_SHIPMENT_TRACKINGS
            error = OrderError()
        })
    }

    @Test
    fun `Request order shipment trackings from api and load cached from db`() {
        doReturn(order).whenever(presenter).orderModel
        doReturn(true).whenever(networkStatus).isConnected()
        presenter.takeView(view)

        presenter.loadOrderDetail(order.getIdentifier())
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
        verify(presenter, times(1)).requestShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Do not request order shipment trackings from api when not connected`() {
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()
        presenter.takeView(view)

        presenter.loadOrderDetail(order.getIdentifier())
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
        verify(presenter, times(0)).requestShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Request fresh shipment tracking from api on network connected event if using non-updated cached data`() {
        doReturn(true).whenever(presenter).isUsingCachedShipmentTrackings
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(1)).requestShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Do not refresh shipment trackings on network connected event if cached data already refreshed`() {
        doReturn(false).whenever(presenter).isUsingCachedShipmentTrackings
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(0)).requestShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Add Order shipment tracking request correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel

        presenter.pushShipmentTrackingProvider(
                provider = "Anitaa Test",
                trackingNum = "123456",
                dateShipped = "2019-05-13T16:11:13Z"
        )
        verify(view).showAddShipmentTrackingSnack()
    }

    @Test
    fun `Show offline message on request to add order shipment tracking if not connected`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()

        presenter.pushShipmentTrackingProvider(
                provider = "Anitaa Test",
                trackingNum = "123456",
                dateShipped = "2019-05-13T16:11:13Z"
        )

        verify(view, times(0)).showAddShipmentTrackingSnack()
        verify(uiMessageResolver, times(1)).showOfflineSnack()
    }
}
