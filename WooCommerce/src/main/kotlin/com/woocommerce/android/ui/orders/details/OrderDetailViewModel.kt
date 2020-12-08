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
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.model.hasNonRefundedProducts
import com.woocommerce.android.model.loadProducts
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderNote
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderShipmentTracking
import com.woocommerce.android.ui.orders.OrderNavigationTarget.IssueOrderRefund
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PrintShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.RefundShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartShippingLabelCreationFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewCreateShippingLabelInfo
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
import org.wordpress.android.fluxc.utils.sumBy

@OpenClassOnDebug
class OrderDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val appPrefs: AppPrefs,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val US_COUNTRY_CODE = "US"
    }

    private val navArgs: OrderDetailFragmentArgs by savedState.navArgs()

    private val orderIdSet: OrderIdSet
        get() = navArgs.orderId.toIdSet()

    final lateinit var order: Order
        private set

    // Keep track of the deleted shipment tracking number in case
    // the request to server fails, we need to display an error message
    // and add the deleted tracking number back to the list
    private var deletedOrderShipmentTrackingSet = mutableSetOf<String>()

    final val orderDetailViewStateData = LiveDataDelegate(savedState, ViewState())
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

    private val wooShippingPluginInfo: WooPlugin by lazy {
        orderDetailRepository.getWooServicesPluginInfo()
    }

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

        val orderInDb = orderDetailRepository.getOrder(navArgs.orderId)
        val needToFetch = orderInDb == null || checkIfFetchNeeded(orderInDb)
        launch {
            if (needToFetch) {
                fetchOrder(true)
            } else {
                orderInDb?.let {
                    order = orderInDb
                    displayOrder()
                }
            }
        }
    }

    private suspend fun displayOrder() {
        updateOrderState()
        loadOrderNotes()
        loadOrderProducts()
        loadOrderRefunds()

        // update the order detail all at once to avoid intermittent showing & hiding of cards
        var viewState = checkShippingLabelRequirements(orderDetailViewState)
        viewState = loadShipmentTracking(viewState)
        viewState = loadOrderShippingLabels(viewState)
        orderDetailViewState = viewState
    }

    private suspend fun fetchOrder(showSkeleton: Boolean) {
        if (networkStatus.isConnected()) {
            orderDetailViewState = orderDetailViewState.copy(
                isOrderDetailSkeletonShown = showSkeleton
            )
            val fetchedOrder = orderDetailRepository.fetchOrder(navArgs.orderId)
            if (fetchedOrder != null) {
                order = fetchedOrder
                updateOrderState()
                loadOrderNotes()
                loadOrderProducts()
                fetchOrderRefunds()

                // update the order detail all at once to avoid intermittent showing & hiding of cards
                var viewState = checkShippingLabelRequirements(orderDetailViewState)
                viewState = loadShipmentTracking(viewState)
                viewState = loadOrderShippingLabels(viewState)
                orderDetailViewState = viewState
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

    // if local order data and refunds are out of sync, it needs to be fetched
    private fun checkIfFetchNeeded(order: Order?): Boolean {
        val refunds = orderDetailRepository.getOrderRefunds(orderIdSet.remoteOrderId)
        return order?.refundTotal.isNotEqualTo(refunds.sumBy { it.amount })
    }

    fun onRefreshRequested() {
        AnalyticsTracker.track(Stat.ORDER_DETAIL_PULLED_TO_REFRESH)
        orderDetailViewState = orderDetailViewState.copy(isRefreshing = true)
        launch { fetchOrder(false) }
    }

    fun hasVirtualProductsOnly(): Boolean {
        return if (order.items.isNotEmpty()) {
            val remoteProductIds = order.items.map { it.productId }
            val products = orderDetailRepository.getProductsByRemoteIds(remoteProductIds)
            products.isNotEmpty() && products.all { it.virtual }
        } else false
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
        triggerEvent(IssueOrderRefund(remoteOrderId = order.remoteId))
    }

    fun onViewRefundedProductsClicked() {
        triggerEvent(ViewRefundedProducts(remoteOrderId = order.remoteId))
    }

    fun onAddOrderNoteClicked() {
        triggerEvent(AddOrderNote(orderIdentifier = order.identifier, orderNumber = order.number))
    }

    fun onRefundShippingLabelClick(shippingLabelId: Long) {
        triggerEvent(RefundShippingLabel(remoteOrderId = order.remoteId, shippingLabelId = shippingLabelId))
    }

    fun onPrintShippingLabelClicked(shippingLabelId: Long) {
        triggerEvent(PrintShippingLabel(remoteOrderId = order.remoteId, shippingLabelId = shippingLabelId))
    }

    fun onAddShipmentTrackingClicked() {
        triggerEvent(
            AddOrderShipmentTracking(
            orderIdentifier = order.identifier,
            orderTrackingProvider = appPrefs.getSelectedShipmentTrackingProviderName(),
            isCustomProvider = appPrefs.getIsSelectedShipmentTrackingProviderCustom()
        ))
    }

    fun onNewShipmentTrackingAdded(shipmentTracking: OrderShipmentTracking) {
        if (networkStatus.isConnected()) {
            val shipmentTrackings = _shipmentTrackings.value?.toMutableList() ?: mutableListOf()
            shipmentTrackings.add(0, shipmentTracking)
            _shipmentTrackings.value = shipmentTrackings

            triggerEvent(ShowSnackbar(string.order_shipment_tracking_added))
            launch {
                AnalyticsTracker.track(
                    ORDER_TRACKING_ADD,
                    mapOf(AnalyticsTracker.KEY_ID to order?.remoteId,
                        AnalyticsTracker.KEY_STATUS to order?.status,
                        AnalyticsTracker.KEY_CARRIER to shipmentTracking.trackingProvider)
                )

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

    fun onShippingLabelRefunded() {
        launch {
            orderDetailViewState = loadOrderShippingLabels(orderDetailViewState)
        }
    }

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
                    order = order.copy(status = CoreOrderStatus.fromValue(newStatus) ?: order.status)
                    orderDetailViewState = orderDetailViewState.copy(order = order)
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
        orderDetailViewState = orderDetailViewState.copy(
            orderStatus = orderDetailRepository.getOrderStatus(order.status.value)
        )
    }

    fun onShippingLabelNoticeTapped() {
        triggerEvent(ViewCreateShippingLabelInfo)
    }

    fun onCreateShippingLabelButtonTapped() {
        AnalyticsTracker.track(Stat.ORDER_DETAIL_CREATE_SHIPPING_LABEL_BUTTON_TAPPED)
        triggerEvent(StartShippingLabelCreationFlow(order.identifier))
    }

    fun onMarkOrderCompleteButtonTapped() {
        AnalyticsTracker.track(Stat.ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED)
        onOrderStatusChanged(CoreOrderStatus.COMPLETED.value)
    }

    private suspend fun updateOrderState() {
        val orderStatus = orderDetailRepository.getOrderStatus(order.status.value)
        orderDetailViewState = orderDetailViewState.copy(
            order = order,
            orderStatus = orderStatus,
            toolbarTitle = resourceProvider.getString(
                string.orderdetail_orderstatus_ordernum, order.number
            )
        )
    }

    private fun checkShippingLabelRequirements(viewState: ViewState): ViewState {
        val storeIsInUs = orderDetailRepository.getStoreCountryCode()?.startsWith(US_COUNTRY_CODE) ?: false
        val isShippingPluginReady = wooShippingPluginInfo.isInstalled && wooShippingPluginInfo.isActive
        val orderHasPhysicalProducts = !hasVirtualProductsOnly()
        val shippingAddressIsInUs = order.shippingAddress.country == US_COUNTRY_CODE
        return viewState.copy(
            isCreateShippingLabelButtonVisible = isShippingPluginReady && storeIsInUs && shippingAddressIsInUs &&
                orderHasPhysicalProducts
        )
    }

    private suspend fun loadOrderNotes() {
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

    private suspend fun fetchOrderRefunds() {
        _orderRefunds.value = orderDetailRepository.fetchOrderRefunds(orderIdSet.remoteOrderId)
        refreshNonRefundedProducts()
    }

    private suspend fun loadOrderRefunds() {
        _orderRefunds.value = orderDetailRepository.getOrderRefunds(orderIdSet.remoteOrderId)
        refreshNonRefundedProducts()
    }

    private suspend fun refreshNonRefundedProducts() {
        val products = _orderRefunds.value?.let { refunds ->
            if (refunds.hasNonRefundedProducts(order.items)) {
                refunds.getNonRefundedProducts(order.items)
            } else emptyList()
        } ?: order.items

        if (products.isEmpty()) {
            orderDetailViewState = orderDetailViewState.copy(isProductListVisible = false)
        } else {
            orderDetailViewState = orderDetailViewState.copy(
                isProductListVisible = orderDetailViewState.areShippingLabelsVisible != true
            )
            _productList.value = products
        }
    }

    private suspend fun loadOrderProducts() {
        // local DB might be missing some products, which need to be fetched
        val productIds = order.items.map { it.productId }
        val numLocalProducts = orderDetailRepository.getProductsByRemoteIds(productIds).count()
        if (numLocalProducts != order.items.size) {
            orderDetailRepository.fetchProductsByRemoteIds(productIds)
        }
    }

    private suspend fun loadShipmentTracking(viewState: ViewState): ViewState {
        if (hasVirtualProductsOnly()) return viewState.copy(isShipmentTrackingAvailable = false)

        return when (orderDetailRepository.fetchOrderShipmentTrackingList(orderIdSet.id, orderIdSet.remoteOrderId)) {
            RequestResult.SUCCESS -> {
                _shipmentTrackings.value = orderDetailRepository.getOrderShipmentTrackings(orderIdSet.id)
                viewState.copy(isShipmentTrackingAvailable = true)
            }
            else -> {
                viewState.copy(isShipmentTrackingAvailable = false)
            }
        }
    }

    private suspend fun loadOrderShippingLabels(viewState: ViewState): ViewState {
        orderDetailRepository.getOrderShippingLabels(orderIdSet.remoteOrderId)
            .whenNotNullNorEmpty {
                _shippingLabels.value = it.loadProducts(order.items)

                // hide the shipment tracking section and the product list section if
                // shipping labels are available for the order
                return viewState.copy(
                    isShipmentTrackingAvailable = false,
                    isProductListVisible = false,
                    areShippingLabelsVisible = true
                )
            }

        orderDetailRepository
            .fetchOrderShippingLabels(orderIdSet.remoteOrderId)
            .loadProducts(order.items)
            .whenNotNullNorEmpty {
                _shippingLabels.value = it

                // hide the shipment tracking section and the product list section if
                // shipping labels are available for the order
                return viewState.copy(
                    isShipmentTrackingAvailable = false,
                    isProductListVisible = false,
                    areShippingLabelsVisible = true
                )
            }

        return viewState.copy(areShippingLabelsVisible = false)
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductImageChanged(event: OnProductImageChanged) {
        orderDetailViewState = orderDetailViewState.copy(refreshedProductId = event.remoteProductId)
    }

    @Parcelize
    data class ViewState(
        val order: Order? = null,
        val toolbarTitle: String? = null,
        val orderStatus: OrderStatus? = null,
        val isOrderDetailSkeletonShown: Boolean? = null,
        val isOrderNotesSkeletonShown: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isShipmentTrackingAvailable: Boolean? = null,
        val refreshedProductId: Long? = null,
        val isCreateShippingLabelButtonVisible: Boolean? = null,
        val isProductListVisible: Boolean? = null,
        val areShippingLabelsVisible: Boolean? = null
    ) : Parcelable {
        val isMarkOrderCompleteButtonVisible: Boolean?
            get() = if (orderStatus != null) orderStatus.statusKey == CoreOrderStatus.PROCESSING.value else null

        val isCreateShippingLabelBannerVisible: Boolean
            get() = isCreateShippingLabelButtonVisible == true && isProductListVisible == true

        val isReprintShippingLabelBannerVisible: Boolean
            get() = !isCreateShippingLabelBannerVisible && areShippingLabelsVisible == true
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<OrderDetailViewModel>
}
