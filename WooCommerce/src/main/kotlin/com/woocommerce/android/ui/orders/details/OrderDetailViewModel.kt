package com.woocommerce.android.ui.orders.details

import android.os.Parcelable
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.Callback
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.model.hasNonRefundedProducts
import com.woocommerce.android.model.loadProducts
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderNote
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderShipmentTracking
import com.woocommerce.android.ui.orders.OrderNavigationTarget.IssueOrderRefund
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PrintShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.RefundShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository.OnProductImageChanged
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.model.order.OrderIdSet
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

@OpenClassOnDebug
class OrderDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val appPrefs: AppPrefs,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: OrderDetailFragmentArgs by savedState.navArgs()

    private val orderIdSet: OrderIdSet
        get() = navArgs.orderId.toIdSet()

    val toolbarTitle: String
        get() = orderDetailViewState.toolbarTitle ?: ""

    val order: Order?
        get() = orderDetailViewState.order

    // Keep track of the deleted shipment tracking number in case
    // the request to server fails, we need to display an error message
    // and add the deleted tracking number back to the list
    private var deletedOrderShipmentTrackingSet = mutableSetOf<String>()

    final val orderDetailViewStateData = LiveDataDelegate(savedState, OrderDetailViewState())
    private var orderDetailViewState by orderDetailViewStateData

    private val _orderNotes = MutableLiveData<List<OrderNote>>()
    val orderNotes: LiveData<List<OrderNote>> = _orderNotes

    private val _orderRefunds = MutableLiveData<List<Refund>>()
    val orderRefunds: LiveData<List<Refund>> = _orderRefunds

    private val _productList = MutableLiveData<List<Order.Item>>()
    val productList: LiveData<List<Order.Item>> = _productList

    private val _shipmentTrackings = MutableLiveData<List<OrderShipmentTracking>>()
    val shipmentTrackings: LiveData<List<OrderShipmentTracking>> = _shipmentTrackings

    private val _shippingLabels = MutableLiveData<List<ShippingLabel>>()
    val shippingLabels: LiveData<List<ShippingLabel>> = _shippingLabels

    override fun onCleared() {
        super.onCleared()
        orderDetailRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    init {
        start()
    }

    final fun start() {
        EventBus.getDefault().register(this)
        orderDetailRepository.getOrder(navArgs.orderId)?.let { orderInDb ->
            updateOrderState(orderInDb)
            loadOrderNotes()
            loadOrderRefunds()
            loadShipmentTrackings()
            loadOrderShippingLabels()
        } ?: launch { fetchOrder(true) }
    }

    fun onRefreshRequested() {
        AnalyticsTracker.track(Stat.ORDER_DETAIL_PULLED_TO_REFRESH)
        orderDetailViewState = orderDetailViewState.copy(isRefreshing = true)
        launch { fetchOrder(false) }
    }

    fun hasVirtualProductsOnly(): Boolean {
        return orderDetailViewState.order?.items?.let { lineItems ->
            val remoteProductIds = lineItems.map { it.productId }
            orderDetailRepository.getProductsByRemoteIds(remoteProductIds).any { it.virtual }
        } ?: false
    }

    fun onEditOrderStatusSelected() {
        orderDetailViewState.orderStatus?.let { orderStatus ->
            triggerEvent(
                ViewOrderStatusSelector(
                    currentStatus = orderStatus.statusKey,
                    orderStatusList = orderDetailRepository.getOrderStatusOptions().toTypedArray()
                ))
        }
    }

    fun onIssueOrderRefundClicked() {
        order?.let { triggerEvent(IssueOrderRefund(remoteOrderId = it.remoteId)) }
    }

    fun onViewRefundedProductsClicked() {
        order?.let { triggerEvent(ViewRefundedProducts(remoteOrderId = it.remoteId)) }
    }

    fun onAddOrderNoteClicked() {
        order?.let { triggerEvent(AddOrderNote(orderIdentifier = it.identifier, orderNumber = it.number)) }
    }

    fun onRefundShippingLabelClick(shippingLabelId: Long) {
        order?.let { triggerEvent(RefundShippingLabel(remoteOrderId = it.remoteId, shippingLabelId = shippingLabelId)) }
    }

    fun onPrintShippingLabelClicked(shippingLabelId: Long) {
        order?.let { triggerEvent(PrintShippingLabel(remoteOrderId = it.remoteId, shippingLabelId = shippingLabelId)) }
    }

    fun onAddShipmentTrackingClicked() {
        order?.let {
            triggerEvent(
                AddOrderShipmentTracking(
                orderIdentifier = it.identifier,
                orderTrackingProvider = appPrefs.getSelectedShipmentTrackingProviderName(),
                isCustomProvider = appPrefs.getIsSelectedShipmentTrackingProviderCustom()
            ))
        }
    }

    fun onNewShipmentTrackingAdded(shipmentTracking: OrderShipmentTracking) {
        if (networkStatus.isConnected()) {
            val shipmentTrackings = _shipmentTrackings.value?.toMutableList() ?: mutableListOf()
            shipmentTrackings.add(0, shipmentTracking)
            _shipmentTrackings.value = shipmentTrackings

            triggerEvent(ShowSnackbar(string.order_shipment_tracking_added))
            launch {
                val addedShipmentTracking = orderDetailRepository.addOrderShipmentTracking(
                    orderIdSet.id,
                    orderIdSet.remoteOrderId,
                    shipmentTracking.toDataModel(),
                    shipmentTracking.isCustomProvider
                )
                if (!addedShipmentTracking) {
                    triggerEvent(ShowSnackbar(string.order_shipment_tracking_error))
                    shipmentTrackings.remove(shipmentTracking)
                    _shipmentTrackings.value = shipmentTrackings
                } else {
                    _shipmentTrackings.value = orderDetailRepository.getOrderShipmentTrackings(orderIdSet.id)
                }
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    fun onShippingLabelRefunded() { launch { loadOrderShippingLabels() } }

    fun onOrderItemRefunded() {
        launch { fetchOrder(false) }
    }

    fun onOrderStatusChanged(newStatus: String) {
        val snackMessage = when (newStatus) {
            CoreOrderStatus.COMPLETED.value -> resourceProvider.getString(string.order_fulfill_marked_complete)
            else -> resourceProvider.getString(string.order_status_changed_to, newStatus)
        }

        // display undo snackbar
        triggerEvent(ShowUndoSnackbar(
            message = snackMessage,
            undoAction = View.OnClickListener { onOrderStatusChangeReverted() },
            dismissAction = object : Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (event != DISMISS_EVENT_ACTION) {
                        // update the order only if user has not clicked on the undo snackbar
                        updateOrderStatus(newStatus)
                    }
                }
            }
        ))

        // change the order status
        val newOrderStatus = orderDetailRepository.getOrderStatus(newStatus)
        orderDetailViewState = orderDetailViewState.copy(
            orderStatus = newOrderStatus
        )
    }

    fun onNewOrderNoteAdded(orderNote: OrderNote) {
        if (networkStatus.isConnected()) {
            val orderNotes = _orderNotes.value?.toMutableList() ?: mutableListOf()
            orderNotes.add(0, orderNote)
            _orderNotes.value = orderNotes

            triggerEvent(ShowSnackbar(string.add_order_note_added))
            launch {
                if (!orderDetailRepository
                        .addOrderNote(orderIdSet.id, orderIdSet.remoteOrderId, orderNote.toDataModel())
                ) {
                    triggerEvent(ShowSnackbar(string.add_order_note_error))
                    orderNotes.remove(orderNote)
                    _orderNotes.value = orderNotes
                }
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    fun onDeleteShipmentTrackingClicked(trackingNumber: String) {
        if (networkStatus.isConnected()) {
            orderDetailRepository.getOrderShipmentTrackingByTrackingNumber(
                orderIdSet.id, trackingNumber
            )?.let { deletedShipmentTracking ->
                deletedOrderShipmentTrackingSet.add(trackingNumber)

                val shipmentTrackings = _shipmentTrackings.value?.toMutableList() ?: mutableListOf()
                shipmentTrackings.remove(deletedShipmentTracking)
                _shipmentTrackings.value = shipmentTrackings

                triggerEvent(ShowUndoSnackbar(
                    message = resourceProvider.getString(string.order_shipment_tracking_delete_snackbar_msg),
                    undoAction = View.OnClickListener { onDeleteShipmentTrackingReverted(deletedShipmentTracking) },
                    dismissAction = object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            super.onDismissed(transientBottomBar, event)
                            if (event != DISMISS_EVENT_ACTION) {
                                // delete the shipment only if user has not clicked on the undo snackbar
                                deleteOrderShipmentTracking(deletedShipmentTracking)
                            }
                        }
                    }
                ))
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    private fun onDeleteShipmentTrackingReverted(shipmentTracking: OrderShipmentTracking) {
        deletedOrderShipmentTrackingSet.remove(shipmentTracking.trackingNumber)
        val shipmentTrackings = _shipmentTrackings.value?.toMutableList() ?: mutableListOf()
        shipmentTrackings.add(shipmentTracking)
        _shipmentTrackings.value = shipmentTrackings
    }

    private fun deleteOrderShipmentTracking(shipmentTracking: OrderShipmentTracking) {
        launch {
            val deletedShipment = orderDetailRepository.deleteOrderShipmentTracking(
                orderIdSet.id, orderIdSet.remoteOrderId, shipmentTracking.toDataModel()
            )
            if (deletedShipment) {
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_delete_success))
            } else {
                onDeleteShipmentTrackingReverted(shipmentTracking)
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_delete_error))
            }
        }
    }

    fun updateOrderStatus(newStatus: String) {
        if (networkStatus.isConnected()) {
            launch {
                if (orderDetailRepository.updateOrderStatus(orderIdSet.id, orderIdSet.remoteOrderId, newStatus)) {
                    order?.let {
                        orderDetailViewState = orderDetailViewState.copy(
                            order = it.copy(status = CoreOrderStatus.fromValue(newStatus) ?: it.status))
                    }
                } else {
                    onOrderStatusChangeReverted()
                    triggerEvent(ShowSnackbar(string.order_error_update_general))
                }
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    fun onOrderStatusChangeReverted() {
        order?.let {
            orderDetailViewState = orderDetailViewState.copy(
                orderStatus = orderDetailRepository.getOrderStatus(it.status.value)
            )
        }
    }

    private suspend fun fetchOrder(showSkeleton: Boolean) {
        if (networkStatus.isConnected()) {
            orderDetailViewState = orderDetailViewState.copy(
                isOrderDetailSkeletonShown = showSkeleton
            )
            val fetchedOrder = orderDetailRepository.fetchOrder(navArgs.orderId)
            if (fetchedOrder != null) {
                updateOrderState(fetchedOrder)
                loadOrderNotes()
                loadOrderRefunds()
                loadShipmentTrackings()
                loadOrderShippingLabels()
            } else {
                triggerEvent(ShowSnackbar(string.order_error_fetch_generic))
            }
            orderDetailViewState = orderDetailViewState.copy(
                isOrderDetailSkeletonShown = false,
                isRefreshing = false
            )
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
            orderDetailViewState = orderDetailViewState.copy(isOrderDetailSkeletonShown = false)
            orderDetailViewState = orderDetailViewState.copy(
                isOrderDetailSkeletonShown = false,
                isRefreshing = false
            )
        }
    }

    private fun updateOrderState(order: Order) {
        val orderStatus = orderDetailRepository.getOrderStatus(order.status.value)
        orderDetailViewState = orderDetailViewState.copy(
            order = order,
            orderStatus = orderStatus,
            toolbarTitle = resourceProvider.getString(
                string.orderdetail_orderstatus_ordernum, order.number
            )
        )
        loadOrderProducts()
    }

    private fun loadOrderNotes() {
        launch {
            orderDetailViewState = orderDetailViewState.copy(isOrderNotesSkeletonShown = true)
            if (!orderDetailRepository.fetchOrderNotes(orderIdSet.id, orderIdSet.remoteOrderId)) {
                triggerEvent(ShowSnackbar(string.order_error_fetch_notes_generic))
            }
            // fetch order notes from the local db and hide the skeleton view
            _orderNotes.value = orderDetailRepository.getOrderNotes(orderIdSet.id)
            orderDetailViewState = orderDetailViewState.copy(isOrderNotesSkeletonShown = false)
        }
    }

    private fun loadOrderRefunds() {
        _orderRefunds.value = orderDetailRepository.getOrderRefunds(orderIdSet.remoteOrderId)
        launch {
            if (networkStatus.isConnected()) {
                _orderRefunds.value = orderDetailRepository.fetchOrderRefunds(orderIdSet.remoteOrderId)
            }
        }

        // display products only if there are some non refunded items in the list
        loadOrderProducts()
    }

    private fun loadOrderProducts() {
        _productList.value = order?.let { order ->
            _orderRefunds.value?.let { refunds ->
                if (refunds.hasNonRefundedProducts(order.items)) {
                    refunds.getNonRefundedProducts(order.items)
                } else emptyList()
            } ?: order.items
        } ?: emptyList()
    }

    private fun loadShipmentTrackings() {
        launch {
            when (orderDetailRepository.fetchOrderShipmentTrackingList(orderIdSet.id, orderIdSet.remoteOrderId)) {
                RequestResult.SUCCESS -> {
                    _shipmentTrackings.value = orderDetailRepository.getOrderShipmentTrackings(orderIdSet.id)
                    orderDetailViewState = orderDetailViewState.copy(isShipmentTrackingAvailable = true)
                }
                else -> {
                    orderDetailViewState = orderDetailViewState.copy(isShipmentTrackingAvailable = false)
                    _shipmentTrackings.value = emptyList()
                }
            }
        }
    }

    private fun loadOrderShippingLabels() {
        order?.let { order ->
            orderDetailRepository.getOrderShippingLabels(orderIdSet.remoteOrderId)
                .whenNotNullNorEmpty { _shippingLabels.value = it.loadProducts(order.items) }

            launch {
                _shippingLabels.value = orderDetailRepository
                    .fetchOrderShippingLabels(orderIdSet.remoteOrderId)
                    .loadProducts(order.items)
            }
        }

        // hide the shipment tracking section and the product list section if
        // shipping labels are available for the order
        _shippingLabels.value?.whenNotNullNorEmpty {
            _productList.value = emptyList()
            _shipmentTrackings.value = emptyList()
            orderDetailViewState = orderDetailViewState.copy(isShipmentTrackingAvailable = false)
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductImageChanged(event: OnProductImageChanged) {
        orderDetailViewState = orderDetailViewState.copy(refreshedProductId = event.remoteProductId)
    }

    @Parcelize
    data class OrderDetailViewState(
        val order: Order? = null,
        val toolbarTitle: String? = null,
        val orderStatus: OrderStatus? = null,
        val isOrderDetailSkeletonShown: Boolean? = null,
        val isOrderNotesSkeletonShown: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isShipmentTrackingAvailable: Boolean? = null,
        val refreshedProductId: Long? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<OrderDetailViewModel>
}
