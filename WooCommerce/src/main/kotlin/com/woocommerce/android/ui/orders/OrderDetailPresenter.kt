package com.woocommerce.android.ui.orders

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

class OrderDetailPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) : OrderDetailContract.Presenter {
    companion object {
        private val TAG: String = this::class.java.simpleName
    }

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

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier) {
        if (orderIdentifier.isNotEmpty()) {
            orderView?.let {
                orderModel = orderStore.getOrderByIdentifier(orderIdentifier)

                // Fetch order notes
                orderModel?.let { order ->
                    // Load order notes from database if available
                    val notes = orderStore.getOrderNotesForOrder(order)

                    // Display
                    orderView?.showOrderDetail(order, notes)

                    // Fetch order notes from API in case there are changes available
                    val payload = FetchOrderNotesPayload(order, selectedSite.get())
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.isError) {
            // TODO: Notify the user of the problem
            WooLog.e(T.ORDERS, "$TAG - Error fetching order notes : ${event.error.message}")
            return
        }

        if (event.causeOfChange == WCOrderAction.FETCH_ORDER_NOTES) {
            orderModel?.let { order ->
                val notes = orderStore.getOrderNotesForOrder(order)
                orderView?.updateOrderNotes(notes)
            }
        }
    }
}
