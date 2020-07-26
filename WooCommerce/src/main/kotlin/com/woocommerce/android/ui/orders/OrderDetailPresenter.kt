package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Handler
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_NOTE_ADD_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_NOTE_ADD_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_DELETE_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_DELETE_SUCCESS
import com.woocommerce.android.extensions.isVirtualProduct
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderDetailRepository.OrderDetailUiItem
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATIONS_READ
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import javax.inject.Inject

class OrderDetailPresenter @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val uiMessageResolver: UIMessageResolver,
    private val networkStatus: NetworkStatus,
    private val notificationStore: NotificationStore,
    private val orderDetailRepository: OrderDetailRepository
) : OrderDetailContract.Presenter {
    companion object {
        private val TAG: String = OrderDetailPresenter::class.java.simpleName
    }

    override var orderModel: WCOrderModel? = null
    override var orderIdentifier: OrderIdentifier? = null
    override var isUsingCachedNotes = false
    override var deletedOrderShipmentTrackingModel: WCOrderShipmentTrackingModel? = null

    /**
     * Adding another flag here to check if shipment trackings have been fetched from api.
     * This is used to passed to [OrderFulfillmentPresenter] and if true, shipment trackings
     * are fetched from db
     */
    override var isShipmentTrackingsFetched: Boolean = false
    override var isShipmentTrackingsFailed: Boolean = false
    private var pendingRemoteOrderId: Long? = null
    private var pendingMarkReadNotification: NotificationModel? = null

    private var orderView: OrderDetailContract.View? = null
    private var isNotesInit = false
    private var isRefreshingOrderStatusOptions = false

    override val coroutineScope = CoroutineScope(dispatchers.main)

    override fun takeView(view: OrderDetailContract.View) {
        orderView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        super.dropView()
        orderView = null
        isNotesInit = false
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
        orderDetailRepository.onCleanup()
    }

    /**
     * Loading order detail from local database
     */
    override fun loadOrderDetailFromDb(orderIdentifier: OrderIdentifier): WCOrderModel? =
            orderStore.getOrderByIdentifier(orderIdentifier)

    /**
     * displaying the loaded order detail data in UI
     */
    override fun loadOrderDetail(orderIdentifier: OrderIdentifier, markComplete: Boolean) {
        OrderDetailFragment@this.orderIdentifier = orderIdentifier
        if (orderIdentifier.isNotEmpty()) {
            orderModel = loadOrderDetailFromDb(orderIdentifier)
            orderModel?.let { order ->
                orderView?.showOrderDetail(order, isFreshData = false)
                if (markComplete) orderView?.showChangeOrderStatusSnackbar(CoreOrderStatus.COMPLETED.value)
                loadOrderDetailInfo(order)
                loadOrderNotes()
            } ?: fetchOrder(orderIdentifier.toIdSet().remoteOrderId, true)
        }
    }

    override fun loadOrderDetailInfo(order: WCOrderModel) {
        orderModel?.let {
            val cachedOrderDetailUiItem = orderDetailRepository.getOrderDetailInfoFromDb(it)

            // if there are no shipping labels cached in the db, we prefer not to show the product list
            // till it can be fetched from the API
            displayOrderDetailInfo(order, cachedOrderDetailUiItem, cachedOrderDetailUiItem.shippingLabels.isNotEmpty())

            fetchOrderDetailInfo(it)
        }
    }

    override fun fetchOrderDetailInfo(order: WCOrderModel) {
        coroutineScope.launch {
            val freshOrderDetailUiItem = orderDetailRepository.fetchOrderDetailInfo(order)
            displayOrderDetailInfo(order, freshOrderDetailUiItem, true)
        }
    }

    private fun displayOrderDetailInfo(
        order: WCOrderModel,
        orderDetailUiItem: OrderDetailUiItem,
        displayProductList: Boolean
    ) {
        orderView?.showRefunds(orderDetailUiItem.orderModel, orderDetailUiItem.refunds)
        orderView?.showShippingLabels(orderDetailUiItem.orderModel, orderDetailUiItem.shippingLabels)

        // display the product list only if we know for sure,
        // that there are no shipping labels available for the order
        if (displayProductList) {
            orderView?.showProductList(order, orderDetailUiItem.refunds, orderDetailUiItem.shippingLabels)
        }

        // if shipping labels are available, we don't need to display shipment tracking information separately
        if (orderDetailUiItem.shippingLabels.isEmpty()) {
            orderView?.showOrderShipmentTrackings(orderDetailUiItem.shipmentTrackingList)
        }
    }

    override fun refreshOrderAfterDelay(refreshDelay: Long) {
        Handler().postDelayed({
            refreshOrderDetail(false)
        }, refreshDelay)
    }

    override fun loadOrderNotes() {
        orderView?.showOrderNotesSkeleton(true)
        orderModel?.let { order ->
            // Preload order notes from database if available
            fetchAndLoadOrderNotesFromDb()

            if (networkStatus.isConnected()) {
                // Attempt to refresh notes from api in the background
                requestOrderNotesFromApi(order)
            } else {
                // Track so when the device is connected notes can be refreshed
                orderView?.showOrderNotesSkeleton(false)
                isUsingCachedNotes = true
            }
        }
    }

    /**
     * Fetch the order notes from the device database
     * Segregating the fetching from db and displaying to UI into two separate methods
     * for better ui testing
     */
    override fun fetchOrderNotesFromDb(order: WCOrderModel): List<WCOrderNoteModel> {
        return orderStore.getOrderNotesForOrder(order)
    }

    /**
     * Fetch and display the order notes from the device database
     */
    override fun fetchAndLoadOrderNotesFromDb() {
        orderModel?.let { order ->
            val notes = fetchOrderNotesFromDb(order)
            if (isNotesInit) {
                orderView?.updateOrderNotes(notes)
            } else {
                isNotesInit = true
                orderView?.showOrderNotes(notes)
            }
        }
    }

    override fun loadOrderShipmentTrackings() {
        orderModel?.let { order ->
            // Preload trackings from the db if we've already fetched it
            if (isShipmentTrackingsFetched) {
                loadShipmentTrackingsFromDb()
            } else if (networkStatus.isConnected() && !isShipmentTrackingsFailed) {
                // Attempt to refresh trackings from api in the background
                requestShipmentTrackingsFromApi(order)
            }
        }
    }

    /**
     * Fetch the order shipment trackings from the device database
     * Segregating the fetching from db and displaying to UI into two separate methods
     * for better ui testing
     */
    override fun getOrderShipmentTrackingsFromDb(order: WCOrderModel): List<WCOrderShipmentTrackingModel> {
        return orderStore.getShipmentTrackingsForOrder(order)
    }

    /**
     * Fetch and display the order shipment trackings from the device database
     */
    override fun loadShipmentTrackingsFromDb() {
        orderModel?.let { order ->
            val trackings = getOrderShipmentTrackingsFromDb(order)
            orderView?.showOrderShipmentTrackings(trackings)
        }
    }

    override fun refreshOrderDetail(displaySkeleton: Boolean) {
        orderModel?.let {
            fetchOrder(it.remoteOrderId, displaySkeleton)
        }
    }

    override fun fetchOrder(remoteOrderId: Long, displaySkeleton: Boolean) {
        orderView?.showSkeleton(displaySkeleton)
        val payload = WCOrderStore.FetchSingleOrderPayload(selectedSite.get(), remoteOrderId)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchSingleOrderAction(payload))
    }

    /**
     * Returns true if all the products specified in the [WCOrderModel.LineItem] is a virtual product
     * and if product exists in the local cache.
     */
    override fun isVirtualProduct(order: WCOrderModel) = isVirtualProduct(
            selectedSite.get(), order.getLineItemList(), productStore
    )

    override fun getProductsByIds(remoteProductIds: List<Long>) =
        productStore.getProductsByRemoteIds(selectedSite.get(), remoteProductIds)

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

    /**
     * Removes the notification from the system bar if present, fetch the new order notification from the database,
     * and fire the event to mark it as read.
     */
    override fun markOrderNotificationRead(context: Context, remoteNoteId: Long) {
        NotificationHandler.removeNotificationWithNoteIdFromSystemBar(context, remoteNoteId.toString())
        notificationStore.getNotificationByRemoteId(remoteNoteId)?.let {
            // Send event that an order with a matching notification was opened
            AnalyticsTracker.track(Stat.NOTIFICATION_OPEN, mapOf(
                    AnalyticsTracker.KEY_TYPE to AnalyticsTracker.VALUE_ORDER,
                    AnalyticsTracker.KEY_ALREADY_READ to it.read))

            if (!it.read) {
                it.read = true
                pendingMarkReadNotification = it
                val payload = MarkNotificationsReadPayload(listOf(it))
                dispatcher.dispatch(NotificationActionBuilder.newMarkNotificationsReadAction(payload))
            }
        }
    }

    override fun getOrderStatusForStatusKey(key: String): WCOrderStatusModel {
        return orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), key) ?: WCOrderStatusModel().apply {
            statusKey = key
            label = key
        }
    }

    override fun getOrderStatusOptions(): Map<String, WCOrderStatusModel> {
        val options = orderStore.getOrderStatusOptionsForSite(selectedSite.get())
        return if (options.isEmpty()) {
            refreshOrderStatusOptions()
            emptyMap()
        } else {
            options.map { it.statusKey to it }.toMap()
        }
    }

    override fun refreshOrderStatusOptions() {
        // Refresh the order status options from the API
        if (!isRefreshingOrderStatusOptions) {
            isRefreshingOrderStatusOptions = true
            dispatcher.dispatch(
                    WCOrderActionBuilder
                            .newFetchOrderStatusOptionsAction(FetchOrderStatusOptionsPayload(selectedSite.get()))
            )
        }
    }

    override fun deleteOrderShipmentTracking(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel) {
        this.deletedOrderShipmentTrackingModel = wcOrderShipmentTrackingModel
        if (!networkStatus.isConnected()) {
            // Device is not connected. Display generic message and exit. Technically we shouldn't get this far, but
            // just in case...
            uiMessageResolver.showOfflineSnack()
            // re-add the deleted tracking item back to the shipment tracking list
            orderView?.undoDeletedTrackingOnError(deletedOrderShipmentTrackingModel)
            deletedOrderShipmentTrackingModel = null
            return
        }

        orderModel?.let { order ->
            AnalyticsTracker.track(Stat.ORDER_TRACKING_DELETE, mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_ORDER_DETAIL
            ))
            val payload = DeleteOrderShipmentTrackingPayload(selectedSite.get(), order, wcOrderShipmentTrackingModel)
            dispatcher.dispatch(WCOrderActionBuilder.newDeleteOrderShipmentTrackingAction(payload))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.causeOfChange == WCOrderAction.FETCH_SINGLE_ORDER) {
            if (event.isError || (orderIdentifier.isNullOrBlank() && pendingRemoteOrderId == null)) {
                orderView?.showLoadOrderError()
                val message = event.error?.message ?: "empty orderIdentifier"
                WooLog.e(T.ORDERS, "$TAG - Error fetching order : $message")
            } else {
                orderModel = loadOrderDetailFromDb(orderIdentifier!!)
                coroutineScope.launch {
                    orderModel?.let { order ->
                        orderView?.showOrderDetail(order, isFreshData = true)
                        orderView?.showSkeleton(false)
                        loadOrderNotes()
                        fetchOrderDetailInfo(order)
                    } ?: orderView?.showLoadOrderError()
                }
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
                    orderModel = loadOrderDetailFromDb(it.getIdentifier())
                }
                orderView?.markOrderStatusChangedSuccess()
            }

            // if order detail refresh is pending, call refresh order detail
            orderView?.refreshOrderDetail()
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
            fetchAndLoadOrderNotesFromDb()
        } else if (event.causeOfChange == DELETE_ORDER_SHIPMENT_TRACKING) {
            if (event.isError) {
                AnalyticsTracker.track(ORDER_TRACKING_DELETE_FAILED)
                WooLog.e(T.ORDERS, "$TAG - Error deleting order shipment tracking : ${event.error.message}")
                orderView?.showDeleteTrackingErrorSnack()
                orderView?.undoDeletedTrackingOnError(deletedOrderShipmentTrackingModel)
                deletedOrderShipmentTrackingModel = null
            } else {
                AnalyticsTracker.track(ORDER_TRACKING_DELETE_SUCCESS)
                orderView?.markTrackingDeletedOnSuccess()
            }

            // if order detail refresh is pending, call refresh order detail
            orderView?.refreshOrderDetail()
        } else if (event.causeOfChange == ADD_ORDER_SHIPMENT_TRACKING) {
            if (event.isError) {
                AnalyticsTracker.track(
                        ORDER_TRACKING_ADD_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message))

                WooLog.e(T.ORDERS, "$TAG - Error posting order note : ${event.error.message}")
                orderView?.showAddAddShipmentTrackingErrorSnack()
            } else {
                AnalyticsTracker.track(ORDER_TRACKING_ADD_SUCCESS)
            }

            // note that we refresh even on error to make sure the transient tracking provider is removed
            // from the tracking list
            loadShipmentTrackingsFromDb()
        }
    }

    /**
     * Request a fresh copy of order notes from the api.
     */
    fun requestOrderNotesFromApi(order: WCOrderModel) {
        val payload = FetchOrderNotesPayload(order, selectedSite.get())
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
    }

    /**
     * Request a fresh copy of order shipment tracking records from the api.
     */
    fun requestShipmentTrackingsFromApi(order: WCOrderModel) {
        val payload = FetchOrderShipmentTrackingsPayload(selectedSite.get(), order)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentTrackingsAction(payload))
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh order notes now that a connection is active is needed
            orderModel?.let { order ->
                if (isUsingCachedNotes) {
                    requestOrderNotesFromApi(order)
                }

                if (!isShipmentTrackingsFetched) {
                    requestShipmentTrackingsFromApi(order)
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        if (event.causeOfChange == MARK_NOTIFICATIONS_READ) {
            onNotificationMarkedRead(event)
        }
    }

    @Suppress
    @Subscribe(threadMode = MAIN)
    fun onOrderStatusOptionsChanged(event: OnOrderStatusOptionsChanged) {
        isRefreshingOrderStatusOptions = false

        if (event.isError) {
            WooLog.e(T.ORDERS, "$TAG " +
                    "- Error fetching order status options from the api : ${event.error.message}")
            return
        }

        orderView?.refreshOrderStatus()
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        // product was just fetched, show its image
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT && !event.isError) {
            orderView?.refreshProductImages()
            // Refresh the customer info section, once the product information becomes available
            orderModel?.let {
                orderView?.refreshCustomerInfoCard(it)
            }
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
