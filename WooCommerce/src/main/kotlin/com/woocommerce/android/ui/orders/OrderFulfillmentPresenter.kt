package com.woocommerce.android.ui.orders

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_DELETE_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_DELETE_SUCCESS
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.extensions.isVirtualProduct
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import javax.inject.Inject

@OpenClassOnDebug
class OrderFulfillmentPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite,
    private val shippingLabelStore: WCShippingLabelStore,
    private val uiMessageResolver: UIMessageResolver,
    private val networkStatus: NetworkStatus
) : OrderFulfillmentContract.Presenter {
    companion object {
        private val TAG: String = OrderFulfillmentPresenter::class.java.simpleName
    }

    override var orderModel: WCOrderModel? = null
    private var orderView: OrderFulfillmentContract.View? = null
    override var isShipmentTrackingsFetched = false
    override var deletedOrderShipmentTrackingModel: WCOrderShipmentTrackingModel? = null

    override fun takeView(view: OrderFulfillmentContract.View) {
        orderView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier, isShipmentTrackingsFetched: Boolean) {
        this.isShipmentTrackingsFetched = isShipmentTrackingsFetched
        orderView?.let { view ->
            orderModel = getOrderDetailFromDb(orderIdentifier)
            orderModel?.let { order ->
                val refunds = refundStore.getAllRefunds(selectedSite.get(), order.remoteOrderId).map { it.toAppModel() }
                view.showOrderDetail(order, refunds)
            }
        }
    }

    /**
     * Loading order detail from local database.
     * Segregating methods that request data from db for better ui testing
     */
    override fun getOrderDetailFromDb(orderIdentifier: OrderIdentifier): WCOrderModel? =
            orderStore.getOrderByIdentifier(orderIdentifier)

    /**
     * Since order shipment tracking list is already requested in [OrderDetailFragment]
     * we use [isShipmentTrackingsFetched] variable to check if shipment tracking list is already fetched from api
     * If [isShipmentTrackingsFetched] = true, then load from cache
     * If [isShipmentTrackingsFetched] = false, and network not connected, load from cache, if available
     * if [isShipmentTrackingsFetched] = false, then request from api
     */
    override fun loadOrderShipmentTrackings() {
        orderModel?.let { order ->
            if (isShipmentTrackingsFetched) {
                loadShipmentTrackingsFromDb()
            } else {
                if (networkStatus.isConnected()) {
                    fetchShipmentTrackingsFromApi(order)
                } else {
                    loadShipmentTrackingsFromDb()
                }
            }
        }
    }

    override fun fetchShipmentTrackingsFromApi(order: WCOrderModel) {
        val payload = FetchOrderShipmentTrackingsPayload(selectedSite.get(), order)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentTrackingsAction(payload))
    }

    /**
     * Segregating methods that request data from db for better ui testing
     */
    override fun getShipmentTrackingsFromDb(order: WCOrderModel) = orderStore.getShipmentTrackingsForOrder(order)

    override fun loadShipmentTrackingsFromDb() {
        orderModel?.let { order ->
            val trackings = getShipmentTrackingsFromDb(order)
            // display the option to add shipment tracking only if shipping labels are not available
            if (shippingLabelStore.getShippingLabelsForOrder(selectedSite.get(), order.remoteOrderId).isEmpty()) {
                orderView?.showOrderShipmentTrackings(trackings)
            }
        }
    }

    /**
     * Returns true if all the products specified in the [WCOrderModel.LineItem] is a virtual product
     * and if product exists in the local cache.
     */
    override fun isVirtualProduct(order: WCOrderModel) = isVirtualProduct(
            selectedSite.get(), order.getLineItemList(), productStore
    )

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

    override fun deleteOrderShipmentTracking(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel) {
        this.deletedOrderShipmentTrackingModel = wcOrderShipmentTrackingModel
        if (!networkStatus.isConnected()) {
            // Device is not connected. Display generic message and exit. Technically we shouldn't get this far, but
            // just in case...
            uiMessageResolver.showOfflineSnack()
            // re-add the deleted tracking item back to the shipment tracking list
            orderView?.undoDeletedTrackingOnError(deletedOrderShipmentTrackingModel)
            deletedOrderShipmentTrackingModel = null
            return
        }

        orderModel?.let { order ->
            AnalyticsTracker.track(
                    Stat.ORDER_TRACKING_DELETE, mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_ORDER_FULFILL
            ))
            val payload = DeleteOrderShipmentTrackingPayload(selectedSite.get(), order, wcOrderShipmentTrackingModel)
            dispatcher.dispatch(WCOrderActionBuilder.newDeleteOrderShipmentTrackingAction(payload))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
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
                orderModel?.let {
                    isShipmentTrackingsFetched = true
                    loadShipmentTrackingsFromDb()
                }
            }
        } else if (event.causeOfChange == DELETE_ORDER_SHIPMENT_TRACKING) {
            if (event.isError) {
                AnalyticsTracker.track(ORDER_TRACKING_DELETE_FAILED)
                WooLog.e(T.ORDERS, "$TAG - Error deleting order shipment tracking : ${event.error.message}")
                orderView?.showDeleteTrackingErrorSnack()
                orderView?.undoDeletedTrackingOnError(deletedOrderShipmentTrackingModel)
                deletedOrderShipmentTrackingModel = null
            } else {
                AnalyticsTracker.track(ORDER_TRACKING_DELETE_SUCCESS)
                orderView?.markTrackingDeletedOnSuccess()
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh order notes now that a connection is active is needed
            orderModel?.let { order ->
                if (!isShipmentTrackingsFetched) {
                    fetchShipmentTrackingsFromApi(order)
                }
            }
        }
    }
}
