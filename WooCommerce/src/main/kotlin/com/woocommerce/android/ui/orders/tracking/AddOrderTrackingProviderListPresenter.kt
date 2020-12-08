package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class AddOrderTrackingProviderListPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val wcStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : AddOrderTrackingProviderListContract.Presenter {
    companion object {
        private val TAG: String = AddOrderShipmentTrackingPresenter::class.java.simpleName
    }

    override var orderModel: WCOrderModel? = null
    override var isShipmentTrackingProviderListFetched: Boolean = false
    private var providerListView: AddOrderTrackingProviderListContract.View? = null

    override fun takeView(view: AddOrderTrackingProviderListContract.View) {
        providerListView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        providerListView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun loadStoreCountryFromDb(): String? = wcStore.getStoreCountryCode(selectedSite.get())

    /**
     * Loading order detail from local database.
     * Segregating methods that request data from db for better ui testing
     */
    override fun loadOrderDetailFromDb(orderIdentifier: OrderIdentifier): WCOrderModel? =
            orderStore.getOrderByIdentifier(orderIdentifier)

    /**
     * Load provider list from db.
     * If list not available in cache and if network is connected, fetch list from api
     * If list not available in cache and if network is not connected, display error snack
     */
    override fun loadShipmentTrackingProviders(orderIdentifier: OrderIdentifier?) {
        orderIdentifier?.let {
            orderModel = loadOrderDetailFromDb(it)
        }

        orderModel?.let { order ->
            loadShipmentTrackingProvidersFromDb()
            if (!isShipmentTrackingProviderListFetched) {
                if (networkStatus.isConnected()) {
                    providerListView?.showSkeleton(true)
                    fetchShipmentTrackingProvidersFromApi(order)
                } else {
                    providerListView?.showProviderListErrorSnack(R.string.offline_error)
                }
            }
        }
    }

    override fun loadShipmentTrackingProvidersFromDb() {
        val providers = getShipmentTrackingProvidersFromDb()
        if (!providers.isNullOrEmpty()) {
            isShipmentTrackingProviderListFetched = true
            providerListView?.showProviderList(providers)
        }
    }

    override fun fetchShipmentTrackingProvidersFromApi(order: WCOrderModel) {
        val payload = FetchOrderShipmentProvidersPayload(selectedSite.get(), order)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentProvidersAction(payload))
    }

    override fun getShipmentTrackingProvidersFromDb(): List<WCOrderShipmentProviderModel> {
        return orderStore.getShipmentProvidersForSite(selectedSite.get())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderShipmentProviderChanged(event: OnOrderShipmentProvidersChanged) {
        providerListView?.showSkeleton(false)
        if (event.isError) {
            WooLog.e(T.ORDERS, "$TAG - Error fetching shipment providers : ${event.error.message}")
            providerListView?.showProviderListErrorSnack(
                    R.string.order_shipment_tracking_provider_list_error_fetch_generic
            )
        } else if (event.rowsAffected == 0) {
            WooLog.e(T.ORDERS, "$TAG - Error fetching shipment providers : empty list")
            providerListView?.showProviderListErrorSnack(
                    R.string.order_shipment_tracking_provider_list_error_empty_list
            )
        } else {
            AnalyticsTracker.track(Stat.ORDER_TRACKING_PROVIDERS_LOADED)
            loadShipmentTrackingProvidersFromDb()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh order tracking providers list now that a connection is active
            orderModel?.let { order ->
                if (!isShipmentTrackingProviderListFetched) {
                    fetchShipmentTrackingProvidersFromApi(order)
                }
            }
        }
    }
}
