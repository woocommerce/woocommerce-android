package com.woocommerce.android.ui.orders

import android.util.Log
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

class OrderDetailPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore
) : OrderDetailContract.Presenter {
    private var orderView: OrderDetailContract.View? = null
    private var orderModel: WCOrderModel? = null

    override fun takeView(view: OrderDetailContract.View) {
        orderView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
    }

    override fun loadOrderDetail(orderId: Int) {
        if (orderId != 0) {
            orderView?.let {
                orderModel = orderStore.getOrderByLocalOrderId(orderId)

                // Fetch order notes
                orderModel?.let { order ->
                    // Load order notes from database if available
                    val notes = orderStore.getOrderNotesForOrder(order)

                    // Display
                    orderView?.showOrderDetail(order, notes)

                    // Fetch order notes from API in case there are changes available
                    orderView?.getSelectedSite()?.let { site ->
                        val payload = FetchOrderNotesPayload(order, site)
                        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
                    }
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.isError) {
            // TODO: Notify the user of the problem
            Log.e(this::class.java.simpleName, "Error fetching order notes : ${event.error.message}")
            return
        }

        if (event.causeOfChange == WCOrderAction.FETCH_ORDER_NOTES && event.rowsAffected > 0) {
            orderModel?.let { order ->
                val notes = orderStore.getOrderNotesForOrder(order)
                orderView?.updateOrderNotes(notes)
            }
        }
    }
}
