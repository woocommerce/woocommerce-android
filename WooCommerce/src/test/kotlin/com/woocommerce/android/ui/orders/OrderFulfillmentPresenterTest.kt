package com.woocommerce.android.ui.orders

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
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
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `Load order shipment tracking card from cache if data already fetched`() {
        val orderTrackingList = OrderTestUtils.generateOrderShipmentTrackings(3, order.id)

        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        doReturn(orderTrackingList).whenever(orderStore).getShipmentTrackingsForOrder(any())

        // order shipment tracking is already fetched from api
        presenter.loadOrderDetail(order.getIdentifier(), true)

        // fetch order shipment trackings
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
        verify(presenter, times(0)).requestShipmentTrackingsFromApi(any())
        verify(dispatcher, times(0)).dispatch(any<Action<*>>())

        // verify that shipment tracking card is displayed
        verify(view).showOrderShipmentTrackings(orderTrackingList)
    }

    @Test
    fun `Request order shipment trackings from api if data not already fetched and network connected - success`() {
        val orderTrackingList = OrderTestUtils.generateOrderShipmentTrackings(3, order.id)

        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        doReturn(orderTrackingList).whenever(orderStore).getShipmentTrackingsForOrder(any())

        // order shipment tracking is not fetched from api
        presenter.loadOrderDetail(order.getIdentifier(), false)

        // fetch order shipment trackings
        assertFalse(presenter.isShipmentTrackingsFetched)
        verify(presenter, times(0)).loadShipmentTrackingsFromDb()
        verify(presenter, times(1)).requestShipmentTrackingsFromApi(any())
        verify(dispatcher, times(1)).dispatch(any<Action<*>>())

        // OnOrderChanged callback from FluxC with error should trigger error message
        presenter.onOrderChanged(OnOrderChanged(3).apply {
            causeOfChange = FETCH_ORDER_SHIPMENT_TRACKINGS
        })

        // verify that shipment tracking card is displayed
        verify(view).showOrderShipmentTrackings(orderTrackingList)
        assertTrue(presenter.isShipmentTrackingsFetched)
    }

    @Test
    fun `Request order shipment trackings from api if data not already fetched and network connected - failure`() {
        val orderTrackingList = OrderTestUtils.generateOrderShipmentTrackings(3, order.id)

        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        doReturn(orderTrackingList).whenever(orderStore).getShipmentTrackingsForOrder(any())

        // order shipment tracking is not fetched from api
        presenter.loadOrderDetail(order.getIdentifier(), false)

        // fetch order shipment trackings
        assertFalse(presenter.isShipmentTrackingsFetched)
        verify(presenter, times(0)).loadShipmentTrackingsFromDb()
        verify(presenter, times(1)).requestShipmentTrackingsFromApi(any())
        verify(dispatcher, times(1)).dispatch(any<Action<*>>())

        // OnOrderChanged callback from FluxC with error should trigger error message
        presenter.onOrderChanged(OnOrderChanged(3).apply {
            causeOfChange = FETCH_ORDER_SHIPMENT_TRACKINGS
            error = OrderError()
        })

        // verify that shipment tracking card is displayed
        verify(view, never()).showOrderShipmentTrackings(orderTrackingList)
        assertFalse(presenter.isShipmentTrackingsFetched)
    }

    @Test
    fun `Do not request order shipment trackings from api when not connected`() {
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()
        presenter.takeView(view)

        presenter.loadOrderDetail(order.getIdentifier(), true)
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
        verify(presenter, times(0)).requestShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Request fresh shipment tracking from api on network connected event if using non-updated cached data`() {
        doReturn(false).whenever(presenter).isShipmentTrackingsFetched
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(1)).requestShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Do not refresh shipment trackings on network connected event if cached data already refreshed`() {
        doReturn(true).whenever(presenter).isShipmentTrackingsFetched
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(0)).requestShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Add order shipment tracking request correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel

        val defaultShipmentTrackingModel = WCOrderShipmentTrackingModel(id = 1).apply {
            trackingProvider = "Anitaa Test"
            trackingLink = "123456"
            dateShipped = "2019-05-13T16:11:13Z"
        }
        presenter.pushShipmentTrackingProvider(defaultShipmentTrackingModel, false)
        verify(view).showAddShipmentTrackingSnack()
    }

    @Test
    fun `Add order shipment tracking with custom provider name request correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel

        val customShipmentTrackingModel = WCOrderShipmentTrackingModel(id = 1).apply {
            trackingProvider = "Anitaa Inc"
            dateShipped = "2019-05-13T16:11:13Z"
        }
        presenter.pushShipmentTrackingProvider(customShipmentTrackingModel, true)
        verify(view).showAddShipmentTrackingSnack()
    }

    @Test
    fun `Add order shipment tracking with custom provider tracking link request correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel

        val customShipmentTrackingModel = WCOrderShipmentTrackingModel(id = 1).apply {
            trackingProvider = "Anitaa Inc"
            dateShipped = "2019-05-13T16:11:13Z"
            trackingLink = "sample.com"
        }
        presenter.pushShipmentTrackingProvider(customShipmentTrackingModel, true)
        verify(view).showAddShipmentTrackingSnack()
    }

    @Test
    fun `Show offline message on request to add order shipment tracking if not connected`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()

        val defaultShipmentTrackingModel = WCOrderShipmentTrackingModel(id = 1).apply {
            trackingProvider = "Anitaa Test"
            trackingLink = "123456"
            dateShipped = "2019-05-13T16:11:13Z"
        }
        presenter.pushShipmentTrackingProvider(defaultShipmentTrackingModel, false)

        verify(view, times(0)).showAddShipmentTrackingSnack()
        verify(uiMessageResolver, times(1)).showOfflineSnack()
    }
}
