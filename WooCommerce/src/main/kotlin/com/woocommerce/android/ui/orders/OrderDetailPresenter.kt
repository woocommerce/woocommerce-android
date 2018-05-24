package com.woocommerce.android.ui.orders

import android.content.Context
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
import org.wordpress.android.util.NetworkUtils
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

    override fun loadOrderDetail(context: Context, orderIdentifier: OrderIdentifier) {
        if (orderIdentifier.isNotEmpty()) {
            orderView?.let {
                orderModel = orderStore.getOrderByIdentifier(orderIdentifier)
                orderModel?.let { order ->
                    orderView?.let { it.showOrderDetail(order) }
                    loadOrderNotes(context) // load order notes
                }
            }
        }
    }

    override fun loadOrderNotes(context: Context) {
        orderView?.let {
            orderModel?.let { order ->
                // Preload order notes from database if available
                fetchAndLoadNotesFromDb()

                // Attempt to refresh notes from api in the background
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val payload = FetchOrderNotesPayload(order, selectedSite.get())
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
                } else {
                    // No network connectivity, notify user
                    it.showNetworkErrorForNotes()
                }
            }
        }
    }

    override fun updateOrderStatus(context: Context, status: String) {
        orderModel?.let { order ->
            if (NetworkUtils.isNetworkAvailable(context)) {
                val payload = UpdateOrderStatusPayload(order, selectedSite.get(), status)
                dispatcher.dispatch(WCOrderActionBuilder.newUpdateOrderStatusAction(payload))
            } else {
                // Notify user unable to mark order complete due to no connectivity
                orderView?.showNetworkErrorForUpdateOrderStatus()
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.causeOfChange == WCOrderAction.FETCH_ORDER_NOTES) {
            if (event.isError) {
                // User notified in Main Activity
                Log.e(this::class.java.simpleName, "Error fetching order notes : ${event.error.message}")
                return
            }

            // Load notes from the database and sent to the view.
            fetchAndLoadNotesFromDb()
        } else if (event.causeOfChange == UPDATE_ORDER_STATUS) {
            if (event.isError) {
                // User notified in main activity
                Log.e(this::class.java.simpleName, "Error updating order status : ${event.error.message}")
                return
            } else {
                orderView?.orderStatusUpdateSuccess()
            }
        }
    }

    /**
     * Fetch the order notes from the device database.
     */
    private fun fetchAndLoadNotesFromDb() {
        orderModel?.let { order ->
            println("AMANDA-TEST > OrderDetailPresenter.fetchAndLoadNotesFromDb > isNotesInit = $isNotesInit")
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
