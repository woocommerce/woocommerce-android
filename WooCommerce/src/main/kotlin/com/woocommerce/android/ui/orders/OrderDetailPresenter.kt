package com.woocommerce.android.ui.orders

import android.util.Log
import com.woocommerce.android.tools.SelectedSite
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import javax.inject.Inject

class OrderDetailPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) : OrderDetailContract.Presenter {
    override var orderModel: WCOrderModel? = null
    private var orderView: OrderDetailContract.View? = null

    override fun takeView(view: OrderDetailContract.View) {
        orderView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier, markComplete: Boolean) {
        if (orderIdentifier.isNotEmpty()) {
            orderView?.let {
                orderModel = orderStore.getOrderByIdentifier(orderIdentifier)

                // Fetch order notes
                orderModel?.let { order ->
                    // Load order notes from database if available
                    val notes = orderStore.getOrderNotesForOrder(order)

                    // Display
                    orderView?.let {
                        it.showOrderDetail(order, notes)
                        if (markComplete) it.pendingMarkOrderComplete()
                    }

                    // Fetch order notes from API in case there are changes available
                    val payload = FetchOrderNotesPayload(order, selectedSite.get())
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
                }
            }
        }
    }

    override fun doMarkOrderComplete() {
        orderModel?.let { order ->
            val payload = UpdateOrderStatusPayload(order, selectedSite.get(), OrderStatus.COMPLETED)
            dispatcher.dispatch(WCOrderActionBuilder.newUpdateOrderStatusAction(payload))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.causeOfChange == WCOrderAction.FETCH_ORDER_NOTES) {
            if (event.isError) {
                // TODO: Notify the user of the problem
                Log.e(this::class.java.simpleName, "Error fetching order notes : ${event.error.message}")
                return
            }

            orderModel?.let { order ->
                val notes = orderStore.getOrderNotesForOrder(order)
                orderView?.updateOrderNotes(notes)
            }
        } else if (event.causeOfChange == UPDATE_ORDER_STATUS) {
            if (event.isError) {
                // TODO: Notify the user of the problem
                Log.e(this::class.java.simpleName, "Error updating order status : ${event.error.message}")
                orderView?.markOrderCompleteFailed(event.error.message)
                return
            }
            // Order status successful!
            orderView?.markOrderCompleteSuccess()
        }
    }
}
