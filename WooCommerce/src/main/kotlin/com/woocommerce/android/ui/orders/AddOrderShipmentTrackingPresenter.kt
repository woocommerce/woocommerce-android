package com.woocommerce.android.ui.orders

import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingContract.DialogView
import com.woocommerce.android.util.WooLog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import javax.inject.Inject

class AddOrderShipmentTrackingPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : AddOrderShipmentTrackingContract.Presenter {
    companion object {
        private val TAG: String = AddOrderShipmentTrackingPresenter::class.java.simpleName
    }

    override var orderModel: WCOrderModel? = null
    override var orderIdentifier: OrderIdentifier? = null
    override var isUsingCachedShipmentTrackingProviders = false
    private var addTrackingView: AddOrderShipmentTrackingContract.View? = null
    private var addTrackingProviderView: AddOrderShipmentTrackingContract.DialogView? = null

    override fun takeView(view: AddOrderShipmentTrackingContract.View) {
        addTrackingView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        addTrackingView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun takeProviderDialogView(view: DialogView) {
        addTrackingProviderView = view
    }

    override fun dropProviderDialogView() {
        addTrackingProviderView = null
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier) {
        this.orderIdentifier = orderIdentifier
        if (orderIdentifier.isNotEmpty()) {
            orderModel = orderStore.getOrderByIdentifier(orderIdentifier)
        }
    }

    /**
     * Currently we are fetching providers list from the api if the network is connected.
     * Perhaps have a mechanism to load providers based on some time limit?
     * or once every time the app is opened (only if the plugin is available)?
     * or initiate the request only once when add tracking button is clicked, before the dialog is even opened?
     */
    override fun loadShipmentTrackingProviders() {
        orderModel?.let { order ->
            // preload data from db if available
            loadShipmentTrackingProvidersFromDb()

            if (networkStatus.isConnected()) {
                // Attempt to refresh tracking providers from api in the background
                addTrackingProviderView?.showSkeleton(true)
                requestShipmentTrackingProvidersFromApi(order)
            } else {
                // Track so when the device is connected shipment tracking providers list can be refreshed
                isUsingCachedShipmentTrackingProviders = true
            }
        }
    }

    override fun requestShipmentTrackingProvidersFromApi(order: WCOrderModel) {
        val payload = FetchOrderShipmentProvidersPayload(selectedSite.get(), order)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentProvidersAction(payload))
    }

    override fun loadShipmentTrackingProvidersFromDb() {
        val providers = orderStore.getShipmentProvidersForSite(selectedSite.get())
        addTrackingProviderView?.showProviderList(providers)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderShipmentProviderChanged(event: OnOrderShipmentProvidersChanged) {
        addTrackingProviderView?.showSkeleton(false)
        if (event.isError) {
            WooLog.e(T.ORDERS, "$TAG - Error fetching order notes : ${event.error.message}")
            addTrackingProviderView?.showProviderListErrorSnack()
        } else {
            isUsingCachedShipmentTrackingProviders = false
            loadShipmentTrackingProvidersFromDb()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh order tracking providers list now that a connection is active
            orderModel?.let { order ->
                if (isUsingCachedShipmentTrackingProviders) {
                    requestShipmentTrackingProvidersFromApi(order)
                }
            }
        }
    }
}
