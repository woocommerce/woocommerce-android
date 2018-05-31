package com.woocommerce.android.ui.orders

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
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
    companion object {
        private val TAG: String = OrderDetailPresenter::class.java.simpleName
    }

    override var orderModel: WCOrderModel? = null
  
    private var orderView: OrderDetailContract.View? = null
    private var isNotesInit = false

    override fun takeView(view: OrderDetailContract.View) {
        orderView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        orderView = null
        isNotesInit = false
        dispatcher.unregister(this)
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier, markComplete: Boolean) {
        if (orderIdentifier.isNotEmpty()) {
            orderView?.let { view ->
                orderModel = orderStore.getOrderByIdentifier(orderIdentifier)?.also { order ->
                    view.showOrderDetail(order)
                }
                if (markComplete) view.showUndoOrderCompleteSnackbar()
                loadOrderNotes() // load order notes
            }
        }
    }

    override fun loadOrderNotes() {
        orderModel?.let { order ->
            // Preload order notes from database if available
            fetchAndLoadNotesFromDb()

            // Attempt to refresh notes from api in the background
            val payload = FetchOrderNotesPayload(order, selectedSite.get())
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
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
                WooLog.e(T.ORDERS, "$TAG - Error fetching order notes : ${event.error.message}")
            } else {
                orderModel?.let { order ->
                    val notes = orderStore.getOrderNotesForOrder(order)
                    orderView?.updateOrderNotes(notes)
                }
            }
        } else if (event.causeOfChange == UPDATE_ORDER_STATUS) {
            if (event.isError) {
                // TODO: Notify the user of the problem
                WooLog.e(T.ORDERS, "$TAG - Error updating order status : ${event.error.message}")
                orderView?.markOrderCompleteFailed()
            } else {
                // Successfully marked order as complete
                orderModel?.let {
                    orderModel = orderStore.getOrderByIdentifier(it.getIdentifier())
                }
                orderView?.markOrderCompleteSuccess()
            }
        }
    }

    /**
     * Fetch the order notes from the device database.
     */
    private fun fetchAndLoadNotesFromDb() {
        orderModel?.let { order ->
            val notes = orderStore.getOrderNotesForOrder(order)
            if (isNotesInit) {
                orderView?.updateOrderNotes(notes)
            } else {
                isNotesInit = true
                orderView?.showOrderNotes(notes)
            }
        }
    }
}
