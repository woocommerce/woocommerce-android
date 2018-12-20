package com.woocommerce.android.ui.orders

import android.content.Context
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_NOTE_ADD
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_NOTE_ADD_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_NOTE_ADD_SUCCESS
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATION_READ
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
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
    private val networkStatus: NetworkStatus,
    private val notificationStore: NotificationStore
) : OrderDetailContract.Presenter {
    companion object {
        private val TAG: String = OrderDetailPresenter::class.java.simpleName
    }

    override var orderModel: WCOrderModel? = null
    override var orderIdentifier: OrderIdentifier? = null
    override var isUsingCachedNotes = false
    private var pendingRemoteOrderId: Long? = null
    private var pendingMarkReadNotification: NotificationModel? = null

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
        this.orderIdentifier = orderIdentifier
        if (orderIdentifier.isNotEmpty()) {
            orderModel = orderStore.getOrderByIdentifier(orderIdentifier)
            orderModel?.let {
                orderView?.showOrderDetail(it)
                if (markComplete) orderView?.showChangeOrderStatusSnackbar(CoreOrderStatus.COMPLETED.value)
                loadOrderNotes()
            } ?: fetchOrder(orderIdentifier.toIdSet().remoteOrderId)
        }
    }

    override fun loadOrderNotes() {
        orderView?.showOrderNotesSkeleton(true)
        orderModel?.let { order ->
            // Preload order notes from database if available
            fetchAndLoadNotesFromDb()

            if (networkStatus.isConnected()) {
                // Attempt to refresh notes from api in the background
                requestOrderNotesFromApi(order)
            } else {
                // Track so when the device is connected notes can be refreshed
                isUsingCachedNotes = true
            }
        }
    }

    override fun fetchOrder(remoteOrderId: Long) {
        orderView?.showLoadOrderProgress(true)
        val payload = WCOrderStore.FetchSingleOrderPayload(selectedSite.get(), remoteOrderId)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchSingleOrderAction(payload))
    }

    override fun doChangeOrderStatus(newStatus: String) {
        if (!networkStatus.isConnected()) {
            // Device is not connected. Display generic message and exit. Technically we shouldn't get this far, but
            // just in case...
            uiMessageResolver.showOfflineSnack()
            return
        }

        orderModel?.let { order ->
            val payload = UpdateOrderStatusPayload(order, selectedSite.get(), newStatus)
            dispatcher.dispatch(WCOrderActionBuilder.newUpdateOrderStatusAction(payload))
        }
    }

    override fun pushOrderNote(noteText: String, isCustomerNote: Boolean) {
        AnalyticsTracker.track(ORDER_NOTE_ADD, mapOf(AnalyticsTracker.KEY_PARENT_ID to orderModel!!.remoteOrderId))

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

    /**
     * Removes the notification from the system bar if present, fetch the new order notification from the database,
     * and fire the event to mark it as read.
     */
    override fun markOrderNotificationRead(context: Context, remoteNoteId: Long) {
        NotificationHandler.removeNotificationWithNoteIdFromSystemBar(context, remoteNoteId.toString())
        notificationStore.getNotificationByRemoteId(remoteNoteId)?.let {
            if (!it.read) {
                it.read = true
                pendingMarkReadNotification = it
                val payload = MarkNotificationReadPayload(it)
                dispatcher.dispatch(NotificationActionBuilder.newMarkNotificationReadAction(payload))
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.causeOfChange == WCOrderAction.FETCH_SINGLE_ORDER) {
            if (event.isError || (orderIdentifier.isNullOrBlank() && pendingRemoteOrderId == null)) {
                WooLog.e(T.ORDERS, "$TAG - Error fetching order : ${event.error.message}")
                orderView?.showLoadOrderError()
            } else {
                orderModel = orderStore.getOrderByIdentifier(orderIdentifier!!)
                orderModel?.let { order ->
                    orderView?.showLoadOrderProgress(false)
                    orderView?.showOrderDetail(order)
                    loadOrderNotes()
                } ?: orderView?.showLoadOrderError()
            }
        } else if (event.causeOfChange == WCOrderAction.FETCH_ORDER_NOTES) {
            orderView?.showOrderNotesSkeleton(false)
            if (event.isError) {
                WooLog.e(T.ORDERS, "$TAG - Error fetching order notes : ${event.error.message}")
                orderView?.showNotesErrorSnack()
            } else {
                orderModel?.let { order ->
                    AnalyticsTracker.track(
                            Stat.ORDER_NOTES_LOADED,
                            mapOf(AnalyticsTracker.KEY_ID to order.remoteOrderId))

                    isUsingCachedNotes = false
                    val notes = orderStore.getOrderNotesForOrder(order)
                    orderView?.updateOrderNotes(notes)
                }
            }
        } else if (event.causeOfChange == UPDATE_ORDER_STATUS) {
            if (event.isError) {
                WooLog.e(T.ORDERS, "$TAG - Error updating order status : ${event.error.message}")

                AnalyticsTracker.track(
                        Stat.ORDER_STATUS_CHANGE_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message))

                orderView?.let {
                    it.showOrderStatusChangedError()
                    it.markOrderStatusChangedFailed()
                }
            } else {
                AnalyticsTracker.track(Stat.ORDER_STATUS_CHANGE_SUCCESS)

                // Successfully marked order status changed
                orderModel?.let {
                    orderModel = orderStore.getOrderByIdentifier(it.getIdentifier())
                }
                orderView?.markOrderStatusChangedSuccess()
            }
        } else if (event.causeOfChange == POST_ORDER_NOTE) {
            if (event.isError) {
                AnalyticsTracker.track(
                        ORDER_NOTE_ADD_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message))

                WooLog.e(T.ORDERS, "$TAG - Error posting order note : ${event.error.message}")
                orderView?.showAddOrderNoteErrorSnack()
            } else {
                AnalyticsTracker.track(ORDER_NOTE_ADD_SUCCESS)
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
    fun requestOrderNotesFromApi(order: WCOrderModel) {
        val payload = FetchOrderNotesPayload(order, selectedSite.get())
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh order notes now that a connection is active is needed
            orderModel?.let { order ->
                if (isUsingCachedNotes) {
                    requestOrderNotesFromApi(order)
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        when (event.causeOfChange) {
            MARK_NOTIFICATION_READ -> onNotificationMarkedRead(event)
        }
    }

    private fun onNotificationMarkedRead(event: OnNotificationChanged) {
        pendingMarkReadNotification?.let {
            // We only care about logging an error
            if (event.changedNotificationLocalIds.contains(it.noteId)) {
                if (event.isError) {
                    WooLog.e(NOTIFICATIONS, "$TAG - Error marking new order notification as read!")
                    pendingMarkReadNotification = null
                }
            }
        }
    }
}
