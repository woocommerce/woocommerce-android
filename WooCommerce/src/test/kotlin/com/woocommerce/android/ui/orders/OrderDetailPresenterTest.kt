package com.woocommerce.android.ui.orders

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_SINGLE_ORDER
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchSingleOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderDetailPresenterTest {
    private val orderDetailView: OrderDetailContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val productStore: WCProductStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val uiMessageResolver: UIMessageResolver = mock()
    private val networkStatus: NetworkStatus = mock()
    private val notificationStore: NotificationStore = mock()
    private val orderDetailRepository: OrderDetailRepository = mock()

    private val coroutineDispatchers = CoroutineDispatchers(Unconfined, Unconfined, Unconfined)
    private val order = OrderTestUtils.generateOrder()
    private val orderIdentifier = order.getIdentifier()
    private val orderDetailUiItem = OrderTestUtils.generateOrderDetailUiItem(order)
    private val orderNotes = OrderTestUtils.generateOrderNotes(10, 2, 1)
    private lateinit var presenter: OrderDetailPresenter

    @Before
    fun setup() {
        presenter = spy(
                OrderDetailPresenter(
                        coroutineDispatchers,
                        dispatcher,
                        orderStore,
                        productStore,
                        selectedSite,
                        uiMessageResolver,
                        networkStatus,
                        notificationStore,
                        orderDetailRepository
                )
        )
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Displays the order detail view correctly`() = test {
        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)
        verify(orderDetailView).showOrderDetail(any(), any())
    }

    @Test
    fun `Displays the order notes view correctly`() = test {
        // Presenter should dispatch FETCH_ORDER_NOTES once order detail is fetched
        // from the order store
        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)

        // Fetch notes and fetch order shipment trackings
        verify(dispatcher, times(2)).dispatch(any<Action<*>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI update
        doReturn(orderNotes).whenever(orderStore).getOrderNotesForOrder(any())
        presenter.onOrderChanged(OnOrderChanged(10).apply { causeOfChange = FETCH_ORDER_NOTES })
        verify(orderDetailView).updateOrderNotes(orderNotes)
    }

    @Test
    fun `Display error message on fetch order notes error`() = test {
        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)

        // Fetch notes and fetch order shipment trackings
        verify(dispatcher, times(2)).dispatch(any<Action<*>>())

        // OnOrderChanged callback from FluxC with error should trigger error message
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = FETCH_ORDER_NOTES
            error = OrderError()
        })
        verify(orderDetailView, times(1)).showNotesErrorSnack()
    }

    @Test
    fun `Mark order complete - Displays undo snackbar correctly`() = test {
        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, true)

        verify(orderDetailView, times(1))
                .showChangeOrderStatusSnackbar(CoreOrderStatus.COMPLETED.value)
    }

    @Test
    fun `Mark order complete - Processes success correctly`() = test {
        doReturn(order).whenever(presenter).orderModel
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        // Presenter should dispatch FETCH_ORDER_NOTES once order detail is fetched
        // from the order store
        presenter.takeView(orderDetailView)
        presenter.doChangeOrderStatus(CoreOrderStatus.COMPLETED.value)
        verify(dispatcher, times(1)).dispatch(any<Action<UpdateOrderStatusPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI Update
        presenter.onOrderChanged(OnOrderChanged(1).apply { causeOfChange = UPDATE_ORDER_STATUS })
        verify(orderDetailView, times(1)).markOrderStatusChangedSuccess()
    }

    @Test
    fun `Display error message on mark order complete error`() = test {
        doReturn(order).whenever(presenter).orderModel
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        // Presenter should dispatch FETCH_ORDER_NOTES once order detail is fetched
        // from the order store
        presenter.takeView(orderDetailView)
        presenter.doChangeOrderStatus(CoreOrderStatus.COMPLETED.value)
        verify(dispatcher, times(1)).dispatch(any<Action<UpdateOrderStatusPayload>>())

        // OnOrderChanged callback from FluxC with error should trigger error message
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = UPDATE_ORDER_STATUS
            error = OrderError()
        })
        verify(orderDetailView, times(1)).showOrderStatusChangedError()
    }

    @Test
    fun `Mark order complete - Reverts status after failure correctly`() = test {
        doReturn(order).whenever(presenter).orderModel
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        // Presenter should dispatch FETCH_ORDER_NOTES once order detail is fetched
        // from the order store
        presenter.takeView(orderDetailView)
        presenter.doChangeOrderStatus(CoreOrderStatus.COMPLETED.value)
        verify(dispatcher, times(1)).dispatch(any<Action<UpdateOrderStatusPayload>>())

        // OnOrderChanged callback from FluxC should trigger the appropriate UI Update
        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = UPDATE_ORDER_STATUS
            error = OrderError(message = "Error")
        })
        verify(orderDetailView).markOrderStatusChangedFailed()
    }

    @Test
    fun `Do not mark order complete and just show offline message`() = test {
        presenter.takeView(orderDetailView)
        doReturn(false).whenever(networkStatus).isConnected()

        presenter.doChangeOrderStatus(CoreOrderStatus.COMPLETED.value)
        verify(uiMessageResolver, times(1)).showOfflineSnack()
        verify(presenter, times(0)).fetchAndLoadOrderNotesFromDb()
    }

    @Test
    fun `Do not request order notes from api when not connected`() = test {
        presenter.takeView(orderDetailView)
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()

        presenter.loadOrderNotes()
        verify(presenter, times(1)).fetchAndLoadOrderNotesFromDb()
        verify(presenter, times(0)).requestOrderNotesFromApi(any())
    }

    @Test
    fun `Request fresh notes from api on network connected event if using non-updated cached data`() =
            test {
                doReturn(true).whenever(presenter).isUsingCachedNotes
                doReturn(order).whenever(presenter).orderModel
                presenter.takeView(orderDetailView)

                presenter.onEventMainThread(ConnectionChangeEvent(true))
                verify(presenter, times(1)).requestOrderNotesFromApi(any())
            }

    @Test
    fun `Do not refresh notes on network connected event if cached data already refreshed`() =
            test {
                doReturn(false).whenever(presenter).isUsingCachedNotes
                doReturn(order).whenever(presenter).orderModel
                presenter.takeView(orderDetailView)

                presenter.onEventMainThread(ConnectionChangeEvent(true))
                verify(presenter, times(0)).requestOrderNotesFromApi(any())
            }

    @Test
    fun `Shows and hides the note list skeleton correctly`() = test {
        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)

        dispatcher.dispatch(any<Action<FetchOrderNotesPayload>>())
        verify(orderDetailView).showOrderNotesSkeleton(true)

        presenter.onOrderChanged(OnOrderChanged(10).apply { causeOfChange = FETCH_ORDER_NOTES })
        verify(orderDetailView).showOrderNotesSkeleton(false)
    }

    @Test
    fun `Request order shipment trackings from api`() = test {
        doReturn(order).whenever(presenter).orderModel
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(false).whenever(presenter).isShipmentTrackingsFetched
        doReturn(false).whenever(presenter).isShipmentTrackingsFailed
        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())

        presenter.loadOrderDetail(orderIdentifier, false)
        verify(presenter, times(1)).requestShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Do not request order shipment trackings from api when not connected`() = test {
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()
        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())

        presenter.loadOrderDetail(orderIdentifier, false)
        verify(presenter, times(0)).requestShipmentTrackingsFromApi(any())
    }

    @Test
    fun `Request fresh shipment tracking from api on network connected event if not already fetched`() =
            test {
                doReturn(false).whenever(presenter).isShipmentTrackingsFetched
                doReturn(order).whenever(presenter).orderModel
                presenter.takeView(orderDetailView)

                presenter.onEventMainThread(ConnectionChangeEvent(true))
                verify(presenter, times(1)).requestShipmentTrackingsFromApi(any())
            }

    @Test
    fun `Do not refresh shipment trackings on network connected event if data already fetched`() =
            test {
                doReturn(true).whenever(presenter).isShipmentTrackingsFetched
                doReturn(order).whenever(presenter).orderModel
                presenter.takeView(orderDetailView)

                presenter.onEventMainThread(ConnectionChangeEvent(true))
                verify(presenter, times(0)).requestShipmentTrackingsFromApi(any())
            }

    @Test
    fun `Do not request delete shipment tracking when network is not available`() = test {
        doReturn(false).whenever(networkStatus).isConnected()
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(orderDetailView)

        // call delete shipment tracking
        val mockWCOrderShipmentTrackingModel = WCOrderShipmentTrackingModel(id = 1)
        presenter.deleteOrderShipmentTracking(mockWCOrderShipmentTrackingModel)

        // ensure that offline snack message is displayed
        verify(uiMessageResolver, times(1)).showOfflineSnack()

        // ensure that deleted item is added back to the list
        verify(orderDetailView, times(1)).undoDeletedTrackingOnError(
                mockWCOrderShipmentTrackingModel
        )

        // ensure that dispatcher is not invoked
        verify(dispatcher, times(0)).dispatch(any<Action<*>>())
    }

    @Test
    fun `Request delete shipment tracking when network is available - error`() = test {
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(orderDetailView)

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
        verify(orderDetailView, times(1)).showDeleteTrackingErrorSnack()

        // ensure that deleted item is added back to the list
        verify(orderDetailView, times(1)).undoDeletedTrackingOnError(trackings[0])
    }

    @Test
    fun `Request delete shipment tracking when network is available - success`() = test {
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(orderDetailView)

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
        verify(orderDetailView, times(1)).markTrackingDeletedOnSuccess()
    }

    @Test
    fun `Add order shipment tracking when network is available - success`() = test {
        presenter.takeView(orderDetailView)
        doReturn(order).whenever(presenter).orderModel

        // mock success response
        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = ADD_ORDER_SHIPMENT_TRACKING
        })

        // verify shipment trackings is loaded from db
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
    }

    @Test
    fun `Add order shipment tracking when network is available - error`() = test {
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(orderDetailView)

        // mock error response
        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = ADD_ORDER_SHIPMENT_TRACKING
            error = OrderError()
        })

        // ensure that error snack message is displayed
        verify(orderDetailView, times(1)).showAddAddShipmentTrackingErrorSnack()

        // verify shipment trackings is loaded from db
        verify(presenter, times(1)).loadShipmentTrackingsFromDb()
    }

    @Test
    fun `Verify product is virtual for a single product in an order`() = test {
        order.lineItems = Gson().toJson(listOf(mapOf("product_id" to "290")))
        val products = listOf(WCProductModel(1).apply { virtual = true })
        doReturn(products).whenever(productStore).getProductsByRemoteIds(any(), any())

        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)
        verify(orderDetailView).showOrderDetail(any(), any())

        assertTrue(presenter.isVirtualProduct(order))
    }

    @Test
    fun `Verify product is not virtual for a single product in an order`() = test {
        order.lineItems = Gson().toJson(listOf(mapOf("product_id" to "290")))
        val products = listOf(WCProductModel(1))
        doReturn(products).whenever(productStore).getProductsByRemoteIds(any(), any())

        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)
        verify(orderDetailView).showOrderDetail(any(), any())

        assertFalse(presenter.isVirtualProduct(order))
    }

    @Test
    fun `Verify product is not virtual for multiple products in an order`() = test {
        order.lineItems = Gson().toJson(
                listOf(
                        mapOf("product_id" to "290"),
                        mapOf("product_id" to "291")
                )
        )

        val products = listOf(
                WCProductModel(1).apply { virtual = false },
                WCProductModel(2).apply { virtual = false },
                WCProductModel(3).apply { virtual = true }
        )
        doReturn(products).whenever(productStore).getProductsByRemoteIds(any(), any())

        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)
        verify(orderDetailView).showOrderDetail(any(), any())

        assertFalse(presenter.isVirtualProduct(order))
    }

    @Test
    fun `Verify product is not virtual for empty products in an order`() = test {
        doReturn(emptyList<WCProductModel>()).whenever(productStore)
                .getProductsByRemoteIds(any(), any())

        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)
        verify(orderDetailView).showOrderDetail(any(), any())

        verify(productStore, times(0)).getProductsByRemoteIds(any(), any())
        assertFalse(presenter.isVirtualProduct(order))
    }

    @Test
    fun `Verify product is not virtual for empty productIds in an order`() = test {
        order.lineItems = Gson().toJson(listOf(mapOf(), mapOf(), mapOf("product_id" to null)))
        doReturn(emptyList<WCProductModel>()).whenever(productStore)
                .getProductsByRemoteIds(any(), any())

        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(orderStore).getOrderByIdentifier(any())
        presenter.loadOrderDetail(orderIdentifier, false)
        verify(orderDetailView).showOrderDetail(any(), any())

        assertFalse(presenter.isVirtualProduct(order))
    }

    @Test
    fun `Request order detail refresh when network available - success`() = test {
        presenter.takeView(orderDetailView)
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).getOrderDetailInfoFromDb(any())
        doReturn(orderDetailUiItem).whenever(orderDetailRepository).fetchOrderDetailInfo(any())
        doReturn(order).whenever(presenter).orderModel
        doReturn(order.getIdentifier()).whenever(presenter).orderIdentifier
        doReturn(true).whenever(networkStatus).isConnected()

        // call refresh order detail
        presenter.refreshOrderDetail(true)

        // verify skeleton view is displayed
        verify(orderDetailView, times(1)).showSkeleton(true)

        // ensure that dispatcher is invoked
        verify(dispatcher, times(1)).dispatch(any<Action<FetchSingleOrderPayload>>())

        // mock success response
        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = FETCH_SINGLE_ORDER
        })

        delay(200)

        // verify skeleton view is no longer displayed
        verify(orderDetailView, times(1)).showSkeleton(false)

        // verify order fetched from db is called
        verify(presenter).loadOrderDetailFromDb(any())
        verify(orderDetailView, times(1)).showOrderDetail(order, true)

        // verify order notes/shipment trackings is fetched
        verify(presenter, times(1)).loadOrderNotes()
        verify(presenter, times(1)).loadOrderShipmentTrackings()
    }

    @Test
    fun `Request order detail refresh when network available - error`() = test {
        doReturn(order).whenever(presenter).orderModel
        presenter.takeView(orderDetailView)

        // call refresh order detail
        presenter.refreshOrderDetail(true)

        // verify skeleton view is displayed
        verify(orderDetailView, times(1)).showSkeleton(true)

        // ensure that dispatcher is invoked
        verify(dispatcher, times(1)).dispatch(any<Action<FetchSingleOrderPayload>>())

        // mock success response
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = FETCH_SINGLE_ORDER
            error = OrderError()
        })

        // verify error snack is displayed
        verify(orderDetailView, times(1)).showLoadOrderError()
    }
}
