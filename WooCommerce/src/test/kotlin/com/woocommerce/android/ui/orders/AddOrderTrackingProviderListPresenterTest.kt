package com.woocommerce.android.ui.orders

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
import com.woocommerce.android.ui.orders.tracking.AddOrderTrackingProviderListContract
import com.woocommerce.android.ui.orders.tracking.AddOrderTrackingProviderListPresenter
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WooCommerceStore

class AddOrderTrackingProviderListPresenterTest {
    private val view: AddOrderTrackingProviderListContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val wcStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()

    private val order = OrderTestUtils.generateOrder()
    private val wcOrderShipmentProviderModels = OrderTestUtils.generateOrderShipmentProviders()
    private lateinit var presenter: AddOrderTrackingProviderListPresenter

    @Before
    fun setup() {
        presenter = spy(
            AddOrderTrackingProviderListPresenter(
                dispatcher, orderStore, wcStore, selectedSite, networkStatus
            )
        )
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Shows and hides the provider list skeleton correctly`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(emptyList<WCOrderShipmentProviderModel>()).whenever(orderStore).getShipmentProvidersForSite(any())

        presenter.loadShipmentTrackingProviders(order.getIdentifier())
        verify(view).showSkeleton(true)
        verify(presenter, times(1)).fetchShipmentTrackingProvidersFromApi(any())

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(10))
        verify(view).showSkeleton(false)
    }

    @Test
    fun `Request order shipment providers from api when network not available and local cache data empty - success`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel

        // request providers list from api only if cache data is empty and when network is connected
        // the `getShipmentTrackingProvidersFromDb()` method will return empty the first time and
        // return list of providers the second time
        doReturn(true).whenever(networkStatus).isConnected()
        whenever(presenter.getShipmentTrackingProvidersFromDb())
                .thenReturn(emptyList())
                .thenReturn(wcOrderShipmentProviderModels)

        presenter.loadShipmentTrackingProviders(order.getIdentifier())

        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(1)).fetchShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(1)).dispatch(any<Action<*>>())
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(1))
        verify(view).showProviderList(wcOrderShipmentProviderModels)
    }

    @Test
    fun `Request order shipment providers from api when network not available and local cache data empty - failure`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel

        // request providers list from api only if cache data is empty and when network is connected
        doReturn(true).whenever(networkStatus).isConnected()
        whenever(presenter.getShipmentTrackingProvidersFromDb()).thenReturn(emptyList())

        presenter.loadShipmentTrackingProviders(order.getIdentifier())
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(1)).fetchShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(1)).dispatch(any<Action<*>>())

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(1).apply {
            error = OrderError()
        })
        verify(view, times(1)).showProviderListErrorSnack(any())
    }

    @Test
    fun `Display order shipment providers list when network is not available and cache is empty`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel

        // network is not available
        // the `getShipmentTrackingProvidersFromDb()` method will return empty the first time
        doReturn(false).whenever(networkStatus).isConnected()
        whenever(presenter.getShipmentTrackingProvidersFromDb()).thenReturn(emptyList())

        presenter.loadShipmentTrackingProviders(order.getIdentifier())
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(0)).fetchShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(0)).dispatch(any<Action<*>>())

        verify(view, times(1)).showProviderListErrorSnack(any())
    }

    @Test
    fun `Do not request order shipment providers list if cache is not empty even if network is connected`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel

        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(wcOrderShipmentProviderModels).whenever(orderStore).getShipmentProvidersForSite(any())

        presenter.loadShipmentTrackingProviders(order.getIdentifier())
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(0)).fetchShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(0)).dispatch(any<Action<*>>())
        verify(view, times(1)).showProviderList(wcOrderShipmentProviderModels)
    }

    @Test
    fun `Do not refresh shipment providers on network connected event if cached data already refreshed`() {
        presenter.takeView(view)
        doReturn(order).whenever(presenter).orderModel
        doReturn(wcOrderShipmentProviderModels).whenever(orderStore).getShipmentProvidersForSite(any())

        presenter.loadShipmentTrackingProviders(order.getIdentifier())

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(0)).fetchShipmentTrackingProvidersFromApi(any())
    }

    @Test
    fun `Display error snackbar when provider list is empty`() {
        presenter.takeView(view)
        val event = OnOrderShipmentProvidersChanged(0)
        presenter.onOrderShipmentProviderChanged(event)
        verify(view, times(1)).showProviderListErrorSnack(any())
    }
}
