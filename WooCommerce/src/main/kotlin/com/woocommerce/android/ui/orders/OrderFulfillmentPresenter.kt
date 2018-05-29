package com.woocommerce.android.ui.orders

import android.util.Log
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderFulfillmentContract.View
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import javax.inject.Inject

class OrderFulfillmentPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) : OrderFulfillmentContract.Presenter {
    override var orderModel: WCOrderModel? = null
    private var orderView: OrderFulfillmentContract.View? = null

    override fun takeView(view: View) {
        orderView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier) {
        orderView?.let { view ->
            orderModel = orderStore.getOrderByIdentifier(orderIdentifier)?.also {
                view.showOrderDetail(it)
            }
        }
    }

    override fun markOrderComplete() {
        orderView?.let { view ->
            orderModel?.let { order ->
                if (view.isNetworkConnected()) {
                    view.toggleCompleteButton(isEnabled = false)
                    val payload = UpdateOrderStatusPayload(order, selectedSite.get(), OrderStatus.COMPLETED)
                    dispatcher.dispatch(WCOrderActionBuilder.newUpdateOrderStatusAction(payload))
                } else {
                    // Notify user unable to mark order complete due to no connectivity
                    view.showNetworkConnectivityError()
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.causeOfChange == UPDATE_ORDER_STATUS) {
            if (event.isError) {
                // User notified in main activity
                orderView?.toggleCompleteButton(isEnabled = true)
                return
            } else {
                orderView?.orderFulfilled()
            }
        }
    }
}
