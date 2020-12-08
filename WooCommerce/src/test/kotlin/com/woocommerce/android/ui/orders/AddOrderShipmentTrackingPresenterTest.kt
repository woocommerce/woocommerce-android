package com.woocommerce.android.ui.orders

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingContract
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingPresenter
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged

class AddOrderShipmentTrackingPresenterTest {
    private val view: AddOrderShipmentTrackingContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()

    private val order = OrderTestUtils.generateOrder()
    private lateinit var presenter: AddOrderShipmentTrackingPresenter

    @Before
    fun setup() {
        presenter = spy(
            AddOrderShipmentTrackingPresenter(
                dispatcher, orderStore, selectedSite, networkStatus
            )
        )
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Add order shipment tracking when network is available - success`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).loadOrderDetailFromDb(any())

        val defaultShipmentTrackingModel = WCOrderShipmentTrackingModel(id = 1).apply {
            trackingProvider = "Anitaa Test"
            trackingLink = "123456"
            dateShipped = "2019-05-13T16:11:13Z"
        }
        presenter.pushShipmentTrackingRecord(order.getIdentifier(), defaultShipmentTrackingModel, false)

        // ensure that dispatcher is invoked
        verify(dispatcher, times(1)).dispatch(any<Action<AddOrderShipmentTrackingPayload>>())

        // verify that add shipment tracking snackbar is displayed
        verify(view).showAddShipmentTrackingSnack()

        // mock success response
        presenter.onOrderChanged(OnOrderChanged(1).apply {
            causeOfChange = ADD_ORDER_SHIPMENT_TRACKING
        })
    }

    @Test
    fun `Add order shipment tracking when network is available and order is null`() {
        doReturn(null).whenever(presenter).loadOrderDetailFromDb(any())
        presenter.takeView(view)

        val defaultShipmentTrackingModel = WCOrderShipmentTrackingModel(id = 1).apply {
            trackingProvider = "Anitaa Test"
            trackingLink = "123456"
            dateShipped = "2019-05-13T16:11:13Z"
        }
        presenter.pushShipmentTrackingRecord(order.getIdentifier(), defaultShipmentTrackingModel, false)

        // ensure that dispatcher is not invoked
        verify(dispatcher, times(0)).dispatch(any<Action<AddOrderShipmentTrackingPayload>>())

        // verify that add shipment tracking snackbar is not displayed
        verify(view, times(0)).showAddShipmentTrackingSnack()

        // ensure that error snack message is displayed
        verify(view, times(1)).showAddAddShipmentTrackingErrorSnack()
    }

    @Test
    fun `Show offline message on request to add order shipment tracking if not connected`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).loadOrderDetailFromDb(any())
        doReturn(false).whenever(networkStatus).isConnected()

        val defaultShipmentTrackingModel = WCOrderShipmentTrackingModel(id = 1).apply {
            trackingProvider = "Anitaa Test"
            trackingLink = "123456"
            dateShipped = "2019-05-13T16:11:13Z"
        }
        presenter.pushShipmentTrackingRecord(order.getIdentifier(), defaultShipmentTrackingModel, false)

        verify(view, times(0)).showAddShipmentTrackingSnack()
        verify(view, times(1)).showOfflineSnack()
    }
}
