package com.woocommerce.android.ui.orders

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderFulfillmentPresenterTest {
    private val view: OrderFulfillmentContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val productStore: WCProductStore = mock()
    private val refundStore: WCRefundStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val uiMessageResolver: UIMessageResolver = mock()
    private val networkStatus: NetworkStatus = mock()
    private val shippngLabelStore: WCShippingLabelStore = mock()

    private val order = OrderTestUtils.generateOrder()
    private lateinit var presenter: OrderFulfillmentPresenter

    @Before
    fun setup() {
        presenter = spy(OrderFulfillmentPresenter(
                dispatcher, orderStore, productStore,
            refundStore, selectedSite, shippngLabelStore,
            uiMessageResolver, networkStatus
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
        verify(view).showOrderDetail(order, emptyList())
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
        doReturn(orderTrackingList).whenever(orderStore).getShipmentTrackingsForOrder(any(), any())

        // order shipment tracking is already fetched from api
        presenter.loadOrderDetail(order.getIdentifier(), true)
        presenter.loadOrderShipmentTrackings()

        // fetch order shipment trackings
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
        verify(presenter, times(0)).fetchShipmentTrackingsFromApi(any())
        verify(dispatcher, times(0)).dispatch(any<Action<*>>())

        // verify that shipment tracking card is displayed
        verify(view).showOrderShipmentTrackings(orderTrackingList)
    }

    @Test
    fun `Request order shipment trackings from api if data not already fetched and network connected - success`() {
        val orderTrackingList = OrderTestUtils.generateOrderShipmentTrackings(3, order.id)

        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        doReturn(orderTrackingList).whenever(orderStore).getShipmentTrackingsForOrder(any(), any())

        // order shipment tracking is not fetched from api
        presenter.loadOrderDetail(order.getIdentifier(), false)
        presenter.loadOrderShipmentTrackings()

        // fetch order shipment trackings
        assertFalse(presenter.isShipmentTrackingsFetched)
        verify(presenter, times(0)).loadShipmentTrackingsFromDb()
        verify(presenter, times(1)).fetchShipmentTrackingsFromApi(any())
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
        doReturn(orderTrackingList).whenever(orderStore).getShipmentTrackingsForOrder(any(), any())

        // order shipment tracking is not fetched from api
        presenter.loadOrderDetail(order.getIdentifier(), false)
        presenter.loadOrderShipmentTrackings()

        // fetch order shipment trackings
        assertFalse(presenter.isShipmentTrackingsFetched)
        verify(presenter, times(0)).loadShipmentTrackingsFromDb()
        verify(presenter, times(1)).fetchShipmentTrackingsFromApi(any())
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
        presenter.loadOrderShipmentTrackings()
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
        verify(presenter, times(0)).fetchShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Request fresh shipment tracking from api on network connected event if using non-updated cached data`() {
        doReturn(false).whenever(presenter).isShipmentTrackingsFetched
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(1)).fetchShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Do not refresh shipment trackings on network connected event if cached data already refreshed`() {
        doReturn(true).whenever(presenter).isShipmentTrackingsFetched
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(0)).fetchShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Add order shipment tracking when network is available - success`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel

        // mock success response
        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = ADD_ORDER_SHIPMENT_TRACKING
        })

        // verify shipment trackings is loaded from db
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
    }

    @Test
    fun `Add order shipment tracking when network is available - error`() {
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        // mock error response
        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = ADD_ORDER_SHIPMENT_TRACKING
            error = OrderError()
        })

        // ensure that error snack message is displayed
        verify(view, times(1)).showAddAddShipmentTrackingErrorSnack()

        // verify shipment trackings is loaded from db
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
    }

    @Test
    fun `Do not request delete shipment tracking when network is not available`() {
        doReturn(false).whenever(networkStatus).isConnected()
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        // call delete shipment tracking
        val mockWCOrderShipmentTrackingModel = WCOrderShipmentTrackingModel(id = 1)
        presenter.deleteOrderShipmentTracking(mockWCOrderShipmentTrackingModel)

        // ensure that offline snack message is displayed
        verify(uiMessageResolver, times(1)).showOfflineSnack()

        // ensure that deleted item is added back to the list
        verify(view, times(1)).undoDeletedTrackingOnError(mockWCOrderShipmentTrackingModel)

        // ensure that dispatcher is not invoked
        verify(dispatcher, times(0)).dispatch(any<Action<*>>())
    }

    @Test
    fun `Request delete shipment tracking when network is available - error`() {
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        // call delete shipment tracking
        val trackings = OrderTestUtils.generateOrderShipmentTrackings(
                3, order.id
        )
        presenter.deleteOrderShipmentTracking(trackings[0])

        // ensure that dispatcher is invoked
        verify(dispatcher, times(1)).dispatch(any<Action<DeleteOrderShipmentTrackingPayload>>())

        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = DELETE_ORDER_SHIPMENT_TRACKING
            error = OrderError()
        })

        // ensure that error snack message is displayed
        verify(view, times(1)).showDeleteTrackingErrorSnack()

        // ensure that deleted item is added back to the list
        verify(view, times(1)).undoDeletedTrackingOnError(trackings[0])
    }

    @Test
    fun `Request delete shipment tracking when network is available - success`() {
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(view)

        // call delete shipment tracking
        val trackings = OrderTestUtils.generateOrderShipmentTrackings(
                3, order.id
        )
        presenter.deleteOrderShipmentTracking(trackings[0])

        // ensure that dispatcher is invoked
        verify(dispatcher, times(1)).dispatch(any<Action<DeleteOrderShipmentTrackingPayload>>())

        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = DELETE_ORDER_SHIPMENT_TRACKING
        })

        // ensure that success snack message is displayed
        verify(view, times(1)).markTrackingDeletedOnSuccess()
    }

    @Test
    fun `Verify product is virtual for a single product in an order`() {
        order.lineItems = Gson().toJson(listOf(mapOf("product_id" to "290")))
        val products = listOf(WCProductModel(1).apply { virtual = true })
        doReturn(products).whenever(productStore).getProductsByRemoteIds(any(), any())

        presenter.takeView(view)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(order.getIdentifier())
        verify(view).showOrderDetail(any(), any())

        assertTrue(presenter.isVirtualProduct(order))
    }

    @Test
    fun `Verify product is not virtual for a single product in an order`() {
        order.lineItems = Gson().toJson(listOf(mapOf("product_id" to "290")))
        val products = listOf(WCProductModel(1))
        doReturn(products).whenever(productStore).getProductsByRemoteIds(any(), any())

        presenter.takeView(view)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(order.getIdentifier())
        verify(view).showOrderDetail(any(), any())

        assertFalse(presenter.isVirtualProduct(order))
    }

    @Test
    fun `Verify product is not virtual for multiple products in an order`() {
        order.lineItems = Gson().toJson(listOf(mapOf("product_id" to "290"), mapOf("product_id" to "291")))

        val products = listOf(
                WCProductModel(1).apply { virtual = false },
                WCProductModel(2).apply { virtual = false },
                WCProductModel(3).apply { virtual = true }
        )
        doReturn(products).whenever(productStore).getProductsByRemoteIds(any(), any())

        presenter.takeView(view)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(order.getIdentifier())
        verify(view).showOrderDetail(any(), any())

        assertFalse(presenter.isVirtualProduct(order))
    }

    @Test
    fun `Verify product is not virtual for empty products in an order`() {
        doReturn(emptyList<WCProductModel>()).whenever(productStore).getProductsByRemoteIds(any(), any())

        presenter.takeView(view)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(order.getIdentifier())
        verify(view).showOrderDetail(any(), any())

        verify(productStore, times(0)).getProductsByRemoteIds(any(), any())
        assertFalse(presenter.isVirtualProduct(order))
    }

    @Test
    fun `Verify product is not virtual for empty productIds in an order`() {
        order.lineItems = Gson().toJson(listOf(mapOf(), mapOf(), mapOf("product_id" to null)))
        doReturn(emptyList<WCProductModel>()).whenever(productStore).getProductsByRemoteIds(any(), any())

        presenter.takeView(view)
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(order.getIdentifier())
        verify(view).showOrderDetail(any(), any())

        assertFalse(presenter.isVirtualProduct(order))
    }
}
