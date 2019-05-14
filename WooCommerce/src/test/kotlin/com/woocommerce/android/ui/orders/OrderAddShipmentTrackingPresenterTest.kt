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
import org.wordpress.android.fluxc.store.WooCommerceStore

class OrderAddShipmentTrackingPresenterTest {
    private val view: AddOrderShipmentTrackingContract.View = mock()
    private val dialogView: AddOrderShipmentTrackingContract.DialogView = mock()
    private val dispatcher: Dispatcher = mock()
    private val orderStore: WCOrderStore = mock()
    private val wcStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()

    private val order = OrderTestUtils.generateOrder()
    private val wcOrderShipmentProviderModels = OrderTestUtils.generateOrderShipmentProviders()
    private lateinit var presenter: AddOrderShipmentTrackingPresenter

    @Before
    fun setup() {
        presenter = spy(AddOrderShipmentTrackingPresenter(
                dispatcher, orderStore, wcStore, selectedSite, networkStatus
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

        presenter.loadOrderDetail(order.getIdentifier(), false)
        verify(dialogView).showSkeleton(true)
        verify(presenter, times(1)).requestShipmentTrackingProvidersFromApi(any())

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(10))
        verify(dialogView).showSkeleton(false)
    }

    @Test
    fun `Request order shipment providers from api only when network available - success`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel

        // request providers list from api only if network available
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(wcOrderShipmentProviderModels).whenever(orderStore).getShipmentProvidersForSite(any())

        // request providers list from api only if not already fetched i.e. isTrackingProviderFetched = false
        presenter.loadOrderDetail(order.getIdentifier(), false)
        verify(presenter, times(0)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(1)).requestShipmentTrackingProvidersFromApi(any())

        verify(dispatcher, times(1)).dispatch(any<Action<*>>())

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(1))
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(dialogView, times(1)).showProviderList(wcOrderShipmentProviderModels)
    }

    @Test
    fun `Request order shipment providers from api only when network available - failure`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(wcOrderShipmentProviderModels).whenever(orderStore).getShipmentProvidersForSite(any())

        // request providers list from api only if not already fetched i.e. isTrackingProviderFetched = false
        presenter.loadOrderDetail(order.getIdentifier(), false)
        verify(presenter, times(0)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(1)).requestShipmentTrackingProvidersFromApi(any())

        verify(dispatcher, times(1)).dispatch(any<Action<*>>())

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(1).apply {
            error = OrderError()
        })
        verify(dialogView, times(1)).showProviderListErrorSnack()
        verify(presenter, times(0)).loadShipmentTrackingProvidersFromDb()
    }

    @Test
    fun `Do not request order shipment providers list if already fetched from api`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(wcOrderShipmentProviderModels).whenever(orderStore).getShipmentProvidersForSite(any())

        // isTrackingProviderFetched = true so providers list already fetched
        presenter.loadOrderDetail(order.getIdentifier(), true)
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(presenter, times(0)).requestShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(0)).dispatch(any<Action<*>>())
        verify(dialogView, times(1)).showProviderList(wcOrderShipmentProviderModels)
    }

    @Test
    fun `Load data from cache if network not available and data not already fetched from api`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()
        doReturn(wcOrderShipmentProviderModels).whenever(orderStore).getShipmentProvidersForSite(any())

        presenter.loadOrderDetail(order.getIdentifier(), false)

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(1)).requestShipmentTrackingProvidersFromApi(any())
        verify(dispatcher, times(1)).dispatch(any<Action<*>>())

        presenter.onOrderShipmentProviderChanged(OnOrderShipmentProvidersChanged(1))

        // will be called twice. Once when displaying from cache when no network is available
        // and once after data is fetched from api
        verify(presenter, times(2)).loadShipmentTrackingProvidersFromDb()
        verify(dialogView, times(2)).showProviderList(wcOrderShipmentProviderModels)
    }

    @Test
    fun `Do not refresh shipment providers on network connected event if cached data already refreshed`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()
        doReturn(wcOrderShipmentProviderModels).whenever(orderStore).getShipmentProvidersForSite(any())

        presenter.loadOrderDetail(order.getIdentifier(), true)

        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(presenter, times(0)).requestShipmentTrackingProvidersFromApi(any())
    }

    @Test
    fun `Display error when no cache and no network available`() {
        presenter.takeProviderDialogView(dialogView)
        doReturn(order).whenever(presenter).orderModel
        doReturn(false).whenever(networkStatus).isConnected()
        doReturn(emptyList<WCOrderShipmentProviderModel>()).whenever(orderStore).getShipmentProvidersForSite(any())

        presenter.loadOrderDetail(order.getIdentifier(), false)
        verify(presenter, times(1)).loadShipmentTrackingProvidersFromDb()
        verify(dialogView, times(1)).showProviderListErrorSnack()
    }
}
