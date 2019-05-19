package com.woocommerce.android.ui.orders

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD_SUCCESS
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderFulfillmentContract.View
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

class OrderFulfillmentPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val uiMessageResolver: UIMessageResolver,
    private val networkStatus: NetworkStatus
) : OrderFulfillmentContract.Presenter {
    companion object {
        private val TAG: String = OrderFulfillmentPresenter::class.java.simpleName
    }

    override var orderModel: WCOrderModel? = null
    private var orderView: OrderFulfillmentContract.View? = null
    override var isUsingCachedShipmentTrackings = false

    override fun takeView(view: View) {
        orderView = view
    }

    override fun dropView() {
        orderView = null
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier, isUsingCachedShipmentTrackings: Boolean) {
        this.isUsingCachedShipmentTrackings = isUsingCachedShipmentTrackings
        orderView?.let { view ->
            orderModel = orderStore.getOrderByIdentifier(orderIdentifier)
            orderModel?.let { order ->
                view.showOrderDetail(order)
                loadOrderShipmentTrackings()
            }
        }
    }

    /**
     * Since order shipment tracking list is already requested in [OrderDetailFragment]
     * we use [isUsingCachedShipmentTrackings] variable to check if the list is updated or using
     * only a cached version.
     * If [isUsingCachedShipmentTrackings] = true, if network connected, fetch from api
     * If [isUsingCachedShipmentTrackings] = true, and network not connected, load from cache, if available
     * if [isUsingCachedShipmentTrackings] = false, then load from cache
     */
    override fun loadOrderShipmentTrackings() {
        orderModel?.let { order ->
            if (!isUsingCachedShipmentTrackings) {
                loadShipmentTrackingsFromDb()
            } else {
                if (networkStatus.isConnected()) {
                    requestShipmentTrackingsFromApi(order)
                } else {
                    loadShipmentTrackingsFromDb()
                }
            }
        }
    }

    override fun requestShipmentTrackingsFromApi(order: WCOrderModel) {
        val payload = FetchOrderShipmentTrackingsPayload(selectedSite.get(), order)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentTrackingsAction(payload))
    }

    override fun loadShipmentTrackingsFromDb() {
        orderModel?.let { order ->
            val trackings = orderStore.getShipmentTrackingsForOrder(order)
            orderView?.showOrderShipmentTrackings(trackings)
        }
    }

    override fun pushShipmentTrackingProvider(provider: String, trackingNum: String, dateShipped: String) {
        AnalyticsTracker.track(
                ORDER_TRACKING_ADD,
                mapOf(AnalyticsTracker.KEY_ID to orderModel!!.remoteOrderId,
                        AnalyticsTracker.KEY_STATUS to orderModel!!.status,
                        AnalyticsTracker.KEY_CARRIER to provider)
        )

        if (!networkStatus.isConnected()) {
            // Device is not connected. Display generic message and exit. Technically we shouldn't get this far, but
            // just in case...
            uiMessageResolver.showOfflineSnack()
            return
        }

        val orderShipmentTrackingModel = WCOrderShipmentTrackingModel()
        orderShipmentTrackingModel.trackingNumber = trackingNum
        orderShipmentTrackingModel.dateShipped = dateShipped
        orderShipmentTrackingModel.trackingProvider = provider

        val payload = AddOrderShipmentTrackingPayload(
                selectedSite.get(),
                orderModel!!,
                orderShipmentTrackingModel,
                false
        )
        dispatcher.dispatch(WCOrderActionBuilder.newAddOrderShipmentTrackingAction(payload))

        orderView?.showAddShipmentTrackingSnack()
    }

    override fun markOrderComplete() {
        orderView?.let { view ->
            if (!networkStatus.isConnected()) {
                // Device is offline. Show generic offline message and exit.
                uiMessageResolver.showOfflineSnack()
                return
            }

            view.toggleCompleteButton(false)

            // Start the process of fulfilling the order
            view.fulfillOrder()

            view.toggleCompleteButton(true)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.causeOfChange == ADD_ORDER_SHIPMENT_TRACKING) {
            if (event.isError) {
                AnalyticsTracker.track(
                        ORDER_TRACKING_ADD_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message))

                WooLog.e(T.ORDERS, "$TAG - Error posting order note : ${event.error.message}")
                orderView?.showAddAddShipmentTrackingErrorSnack()
            } else {
                AnalyticsTracker.track(ORDER_TRACKING_ADD_SUCCESS)
            }

            // note that we refresh even on error to make sure the transient tracking provider is removed
            // from the tracking list
            loadShipmentTrackingsFromDb()
        } else if (event.causeOfChange == WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS) {
            if (event.isError) {
                WooLog.e(T.ORDERS, "$TAG - Error fetching order shipment tracking info: ${event.error.message}")
            } else {
                orderModel?.let { order ->
                    isUsingCachedShipmentTrackings = false
                    loadShipmentTrackingsFromDb()
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh order notes now that a connection is active is needed
            orderModel?.let { order ->
                if (isUsingCachedShipmentTrackings) {
                    requestShipmentTrackingsFromApi(order)
                }
            }
        }
    }
}
