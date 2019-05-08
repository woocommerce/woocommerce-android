package com.woocommerce.android.ui.orders

import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderFulfillmentContract.View
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderFulfillmentPresenter @Inject constructor(
    private val orderStore: WCOrderStore,
    private val uiMessageResolver: UIMessageResolver,
    private val networkStatus: NetworkStatus
) : OrderFulfillmentContract.Presenter {
    override var orderModel: WCOrderModel? = null
    private var orderView: OrderFulfillmentContract.View? = null

    override fun takeView(view: View) {
        orderView = view
    }

    override fun dropView() {
        orderView = null
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier) {
        orderView?.let { view ->
            orderModel = orderStore.getOrderByIdentifier(orderIdentifier)?.also {
                view.showOrderDetail(it)
                loadOrderShipmentTrackings(it)
            }
        }
    }

    /**
     * Since shipment tracking info, if available would already be displayed
     * in order detail page, under Tracking section, the data is assumed to be current.
     * So when fetching data here, it is from the db and not from api
     */
    override fun loadOrderShipmentTrackings(order: WCOrderModel) {
        val trackings = orderStore.getShipmentTrackingsForOrder(order)
        orderView?.showOrderShipmentTrackings(trackings)
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
}
