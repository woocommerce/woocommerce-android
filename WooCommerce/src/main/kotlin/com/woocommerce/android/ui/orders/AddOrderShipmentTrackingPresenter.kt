package com.woocommerce.android.ui.orders

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
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

    private var addTrackingView: AddOrderShipmentTrackingContract.View? = null

    override fun takeView(view: AddOrderShipmentTrackingContract.View) {
        addTrackingView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        addTrackingView = null
        dispatcher.unregister(this)
    }

    /**
     * Loading order detail from local database.
     * Segregating methods that request data from db for better ui testing
     */
    override fun loadOrderDetailFromDb(orderIdentifier: OrderIdentifier): WCOrderModel? {
        return orderStore.getOrderByIdentifier(orderIdentifier)
    }

    override fun pushShipmentTrackingRecord(
        orderIdentifier: OrderIdentifier,
        wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel,
        isCustomProvider: Boolean
    ): Boolean {
        if (!networkStatus.isConnected()) {
            addTrackingView?.showOfflineSnack()
            return false
        }

        val order = loadOrderDetailFromDb(orderIdentifier)
        if (order == null) {
            addTrackingView?.showAddAddShipmentTrackingErrorSnack()
            return false
        }

        AnalyticsTracker.track(
                ORDER_TRACKING_ADD,
                mapOf(AnalyticsTracker.KEY_ID to order.remoteOrderId,
                        AnalyticsTracker.KEY_STATUS to order.status,
                        AnalyticsTracker.KEY_CARRIER to wcOrderShipmentTrackingModel.trackingProvider)
        )

        val payload = AddOrderShipmentTrackingPayload(
                selectedSite.get(), order, wcOrderShipmentTrackingModel, isCustomProvider
        )
        dispatcher.dispatch(WCOrderActionBuilder.newAddOrderShipmentTrackingAction(payload))

        addTrackingView?.showAddShipmentTrackingSnack()
        return true
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        // ignore this event as it will be handled by order detail - this is only here because we need
        // at least one @Subscribe in order to register with the dispatcher.
    }
}
