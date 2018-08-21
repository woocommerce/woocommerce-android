package com.woocommerce.android.ui.orders

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import javax.inject.Inject

class OrderDetailPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val uiMessageResolver: UIMessageResolver,
    private val networkStatus: NetworkStatus
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
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        orderView = null
        isNotesInit = false
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier, markComplete: Boolean) {
        if (orderIdentifier.isNotEmpty()) {
            orderView?.let { view ->
                orderModel = orderStore.getOrderByIdentifier(orderIdentifier)?.also { order ->
                    view.showOrderDetail(order)
                }
                if (markComplete) orderView?.showUndoOrderCompleteSnackbar()
                loadOrderNotes() // load order notes
            }
        }
    }

    override fun loadOrderNotes() {
        orderModel?.let { order ->
            // Preload order notes from database if available
            fetchAndLoadNotesFromDb()

            if (networkStatus.isConnected()) {
                // Attempt to refresh notes from api in the background
                requestOrderNotesFromApi(order)
            }
        }
    }

    override fun doMarkOrderComplete() {
        if (!networkStatus.isConnected()) {
            // Device is not connected. Display generic message and exit. Technically we shouldn't get this far, but
            // just in case...
            uiMessageResolver.showOfflineSnack()
            return
        }

        AnalyticsTracker.trackWithSiteDetails(Stat.FULFILLED_ORDER, selectedSite.get())
        orderModel?.let { order ->
            val payload = UpdateOrderStatusPayload(order, selectedSite.get(), CoreOrderStatus.COMPLETED.value)
            dispatcher.dispatch(WCOrderActionBuilder.newUpdateOrderStatusAction(payload))
        }
    }

    override fun pushOrderNote(noteText: String, isCustomerNote: Boolean) {
        if (!networkStatus.isConnected()) {
            // Device is not connected. Display generic message and exit. Technically we shouldn't get this far, but
            // just in case...
            uiMessageResolver.showOfflineSnack()
            return
        }

        val noteModel = WCOrderNoteModel()
        noteModel.isCustomerNote = isCustomerNote
        noteModel.note = noteText

        val payload = WCOrderStore.PostOrderNotePayload(orderModel!!, selectedSite.get(), noteModel)
        dispatcher.dispatch(WCOrderActionBuilder.newPostOrderNoteAction(payload))

        orderView?.showAddOrderNoteSnack()
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.causeOfChange == WCOrderAction.FETCH_ORDER_NOTES) {
            if (event.isError) {
                WooLog.e(T.ORDERS, "$TAG - Error fetching order notes : ${event.error.message}")
                orderView?.showNotesErrorSnack()
            } else {
                orderModel?.let { order ->
                    val notes = orderStore.getOrderNotesForOrder(order)
                    orderView?.updateOrderNotes(notes)
                }
            }
        } else if (event.causeOfChange == UPDATE_ORDER_STATUS) {
            if (event.isError) {
                WooLog.e(T.ORDERS, "$TAG - Error updating order status : ${event.error.message}")
                orderView?.let {
                    it.showCompleteOrderError()
                    it.markOrderCompleteFailed()
                }
            } else {
                // Successfully marked order as complete
                orderModel?.let {
                    orderModel = orderStore.getOrderByIdentifier(it.getIdentifier())
                }
                orderView?.markOrderCompleteSuccess()
            }
        } else if (event.causeOfChange == POST_ORDER_NOTE) {
            if (event.isError) {
                WooLog.e(T.ORDERS, "$TAG - Error posting order note : ${event.error.message}")
                orderView?.showAddOrderNoteErrorSnack()
            }
            // note that we refresh even on error to make sure the transient note is removed
            // from the note list
            fetchAndLoadNotesFromDb()
        }
    }

    /**
     * Fetch the order notes from the device database.
     */
    fun fetchAndLoadNotesFromDb() {
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

    /**
     * Request a fresh copy of order notes from the api.
     */
    private fun requestOrderNotesFromApi(order: WCOrderModel) {
        val payload = FetchOrderNotesPayload(order, selectedSite.get())
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh order notes now that a connection is active
            orderModel?.let { requestOrderNotesFromApi(it) }
        }
    }
}
