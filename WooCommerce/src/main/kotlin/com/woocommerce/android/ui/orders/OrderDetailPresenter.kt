package com.woocommerce.android.ui.orders

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
                    if (markComplete) view.pendingMarkOrderComplete()
                }
                loadOrderNotes() // load order notes
            }
        }
    }

    override fun loadOrderNotes() {
        orderView?.let { view ->
            orderModel?.let { order ->
                // Preload order notes from database if available
                fetchAndLoadNotesFromDb()

                // Attempt to refresh notes from api in the background
                if (view.isNetworkConnected()) {
                    val payload = FetchOrderNotesPayload(order, selectedSite.get())
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
                } else {
                    // No network connectivity, notify user
                    view.showNetworkConnectivityError()
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
            if (!event.isError) {
                // Load notes from the database and sent to the view.
                fetchAndLoadNotesFromDb()
            }
        } else if (event.causeOfChange == UPDATE_ORDER_STATUS) {
            if (event.isError) {
                // User notified of error in main activity
                orderView?.markOrderCompleteFailed()
            } else {
                // Successfully marked order as complete
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
