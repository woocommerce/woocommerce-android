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
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError

class AddOrderShipmentTrackingPresenterTest {
    private val view: AddOrderShipmentTrackingContract.View = mock()
    private val dialogView: AddOrderShipmentTrackingContract.DialogView = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()

    private val order = OrderTestUtils.generateOrder()
    private val wcOrderShipmentProviderModels = OrderTestUtils.generateOrderShipmentProviders()
    private lateinit var presenter: AddOrderShipmentTrackingPresenter

    @Before
    fun setup() {
        presenter = spy(AddOrderShipmentTrackingPresenter(
                dispatcher, orderStore, selectedSite, networkStatus
        ))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Shows and hides the provider list skeleton correctly`() {
        presenter.takeView(view)
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(emptyList<WCOrderShipmentProviderModel>()).whenever(orderStore).getShipmentProvidersForSite(any())

        presenter.loadOrderDetail(order.getIdentifier())
        verify(dialogView).showSkeleton(true)
        verify(presenter, times(1)).requestShipmentTrackingProvidersFromApi(any())

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(10))
        verify(dialogView).showSkeleton(false)
    }

    @Test
    fun `Request order shipment providers from api when network not available and local cache data empty - success`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel

        // request providers list from api only if cache data is empty and when network is connected
        // the `requestShipmentTrackingProvidersFromDb()` method will return empty the first time and
        // return list of providers the second time
        doReturn(true).whenever(networkStatus).isConnected()
        whenever(presenter.requestShipmentTrackingProvidersFromDb())
                .thenReturn(emptyList())
                .thenReturn(wcOrderShipmentProviderModels)

        presenter.loadOrderDetail(order.getIdentifier())

        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(1)).requestShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(1)).dispatch(any<Action<*>>())
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(1))
        verify(dialogView).showProviderList(wcOrderShipmentProviderModels)
    }

    @Test
    fun `Request order shipment providers from api when network not available and local cache data empty - failure`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel

        // request providers list from api only if cache data is empty and when network is connected
        doReturn(true).whenever(networkStatus).isConnected()
        whenever(presenter.requestShipmentTrackingProvidersFromDb()).thenReturn(emptyList())

        presenter.loadOrderDetail(order.getIdentifier())
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(1)).requestShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(1)).dispatch(any<Action<*>>())

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(1).apply {
            error = OrderError()
        })
        verify(dialogView, times(1)).showProviderListErrorSnack(any())
    }

    @Test
    fun `Display order shipment providers list when network is not available and cache is empty`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel

        // network is not available
        // the `requestShipmentTrackingProvidersFromDb()` method will return empty the first time
        doReturn(false).whenever(networkStatus).isConnected()
        whenever(presenter.requestShipmentTrackingProvidersFromDb()).thenReturn(emptyList())

        presenter.loadOrderDetail(order.getIdentifier())
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(0)).requestShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(0)).dispatch(any<Action<*>>())

        verify(dialogView, times(1)).showProviderListErrorSnack(any())
    }

    @Test
    fun `Do not request order shipment providers list if cache is not empty even if network is connected`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel

        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(wcOrderShipmentProviderModels).whenever(orderStore).getShipmentProvidersForSite(any())

        presenter.loadOrderDetail(order.getIdentifier())
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(0)).requestShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(0)).dispatch(any<Action<*>>())
        verify(dialogView, times(1)).showProviderList(wcOrderShipmentProviderModels)
    }

    @Test
    fun `Do not refresh shipment providers on network connected event if cached data already refreshed`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()
        doReturn(wcOrderShipmentProviderModels).whenever(orderStore).getShipmentProvidersForSite(any())

        presenter.loadOrderDetail(order.getIdentifier())

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(0)).requestShipmentTrackingProvidersFromApi(any())
    }
}
