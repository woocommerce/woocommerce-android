package com.woocommerce.android.ui.orders.details

import android.os.Parcelable
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.Callback
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.ShowAdditionalInfo
import com.woocommerce.android.cardreader.CardPaymentStatus.WaitingForInput
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.Order.Status
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.model.getNonRefundedShippingLabelProducts
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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
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
        private const val SUPPORTED_WCS_VERSION = "1.25.11"
    }
    private val navArgs: OrderDetailFragmentArgs by savedState.navArgs()

    private val orderIdSet: OrderIdSet
        get() = navArgs.orderId.toIdSet()

    final var order: Order
        get() = requireNotNull(viewState.order)
        set(value) {
            viewState = viewState.copy(order = value)
        }

    // Keep track of the deleted shipment tracking number in case
    // the request to server fails, we need to display an error message
    // and add the deleted tracking number back to the list
    private var deletedOrderShipmentTrackingSet = mutableSetOf<String>()

    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

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

    private val isShippingPluginReady: Boolean by lazy {
        val pluginInfo = orderDetailRepository.getWooServicesPluginInfo()
        pluginInfo.isInstalled && pluginInfo.isActive &&
            (pluginInfo.version ?: "0.0.0").semverCompareTo(SUPPORTED_WCS_VERSION) >= 0
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
                    displayOrderDetails()
                    fetchAndDisplayOrderDetails()
                }
            }
        }
    }

    private suspend fun fetchAndDisplayOrderDetails() {
        fetchOrderNotes()
        fetchProductAndShippingDetails()
        displayOrderDetails()
    }

    private fun displayOrderDetails() {
        updateOrderState()
        loadOrderNotes()
        displayProductAndShippingDetails()
    }

    private suspend fun fetchOrder(showSkeleton: Boolean) {
        if (networkStatus.isConnected()) {
            viewState = viewState.copy(
                isOrderDetailSkeletonShown = showSkeleton
            )
            val fetchedOrder = orderDetailRepository.fetchOrder(navArgs.orderId)
            if (fetchedOrder != null) {
                order = fetchedOrder
                fetchAndDisplayOrderDetails()
            } else {
                triggerEvent(ShowSnackbar(string.order_error_fetch_generic))
            }
            viewState = viewState.copy(
                isOrderDetailSkeletonShown = false,
                isRefreshing = false
            )
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
            viewState = viewState.copy(isOrderDetailSkeletonShown = false)
            viewState = viewState.copy(
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
        viewState = viewState.copy(isRefreshing = true)
        launch { fetchOrder(false) }
    }

    fun hasVirtualProductsOnly(): Boolean {
        return if (order.items.isNotEmpty()) {
            val remoteProductIds = order.items.map { it.productId }
            orderDetailRepository.hasVirtualProductsOnly(remoteProductIds)
        } else false
    }

    fun onEditOrderStatusSelected() {
        viewState.orderStatus?.let { orderStatus ->
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

    fun onAcceptCardPresentPaymentClicked(cardReaderManager: CardReaderManager?) {
        launch {
            // TODO cardreader use the correct currency or hide the button
            cardReaderManager?.collectPayment(999, "usd")?.collect {
                when (it) {
                    CapturingPaymentFailed,
                    CollectingPaymentFailed,
                    InitializingPaymentFailed,
                    ProcessingPaymentFailed -> triggerEvent(
                        ShowSnackbar(string.generic_string, arrayOf("Payment failed :("))
                    )
                    PaymentCompleted -> triggerEvent(
                        ShowSnackbar(string.generic_string, arrayOf("Payment completed successfully :))"))
                    )
                    CollectingPayment -> {
                    }
                    InitializingPayment -> triggerEvent(
                        ShowSnackbar(string.generic_string, arrayOf("Payment flow started."))
                    )
                    CapturingPayment -> {
                    }
                    ProcessingPayment -> triggerEvent(
                        ShowSnackbar(string.generic_string, arrayOf("Processing payment."))
                    )
                    ShowAdditionalInfo -> {
                    }
                    WaitingForInput -> triggerEvent(
                        ShowSnackbar(string.generic_string, arrayOf("Tap your card"))
                    )
                }
            }
        }
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
        AnalyticsTracker.track(
            ORDER_TRACKING_ADD,
            mapOf(AnalyticsTracker.KEY_ID to order.remoteId,
                AnalyticsTracker.KEY_STATUS to order.status,
                AnalyticsTracker.KEY_CARRIER to shipmentTracking.trackingProvider)
        )
        _shipmentTrackings.value = orderDetailRepository.getOrderShipmentTrackings(orderIdSet.id)
    }

    fun onShippingLabelRefunded() {
        launch {
            fetchOrderShippingLabelsAsync().await()
            displayOrderDetails()
        }
    }

    fun onShippingLabelsPurchased() {
        // Refresh UI from the database, as new labels are cached by FluxC after the purchase,
        // if for any reason, the order wasn't found, refetch it
        orderDetailRepository.getOrder(navArgs.orderId)?.let {
            order = it
            displayOrderDetails()
        } ?: launch { fetchOrder(true) }
    }

    fun onOrderItemRefunded() {
        launch { fetchOrder(false) }
    }

    fun onOrderStatusChanged(newStatus: String) {
        val snackMessage = when (newStatus) {
            CoreOrderStatus.COMPLETED.value -> resourceProvider.getString(string.order_fulfill_marked_complete)
            else -> resourceProvider.getString(string.order_status_changed_to, newStatus)
        }

        AnalyticsTracker.track(Stat.ORDER_STATUS_CHANGE, mapOf(
            AnalyticsTracker.KEY_ID to order.remoteId,
            AnalyticsTracker.KEY_FROM to order.status.value,
            AnalyticsTracker.KEY_TO to newStatus))

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
        viewState = viewState.copy(
            orderStatus = newOrderStatus
        )
    }

    fun onNewOrderNoteAdded(orderNote: OrderNote) {
        val orderNotes = _orderNotes.value?.toMutableList() ?: mutableListOf()
        orderNotes.add(0, orderNote)
        _orderNotes.value = orderNotes
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
                    order = order.copy(status = Status.fromValue(newStatus))
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
        viewState = viewState.copy(
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

    private fun updateOrderState() {
        val orderStatus = orderDetailRepository.getOrderStatus(order.status.value)
        viewState = viewState.copy(
            order = order,
            orderStatus = orderStatus,
            toolbarTitle = resourceProvider.getString(
                string.orderdetail_orderstatus_ordernum, order.number
            )
        )
    }

    private fun loadOrderNotes() {
        _orderNotes.value = orderDetailRepository.getOrderNotes(orderIdSet.id)
    }

    private fun fetchOrderNotes() {
        launch {
            if (!orderDetailRepository.fetchOrderNotes(orderIdSet.id, orderIdSet.remoteOrderId)) {
                triggerEvent(ShowSnackbar(string.order_error_fetch_notes_generic))
            }
            // fetch order notes from the local db and hide the skeleton view
            _orderNotes.value = orderDetailRepository.getOrderNotes(orderIdSet.id)
        }
    }

    private fun loadOrderRefunds(): ListInfo<Refund> {
        return ListInfo(list = orderDetailRepository.getOrderRefunds(orderIdSet.remoteOrderId))
    }

    private fun loadOrderProducts(
        refunds: ListInfo<Refund>,
        shippingLabels: ListInfo<ShippingLabel>
    ): ListInfo<Order.Item> {
        val products = if (shippingLabels.isVisible) {
            // If there are some products not associated with any shipping labels (when shipping labels
            // are refunded, for instance), the products card should be displayed with those products
            shippingLabels.list.getNonRefundedShippingLabelProducts()
        } else {
            refunds.list.getNonRefundedProducts(order.items)
        }

        return ListInfo(isVisible = products.isNotEmpty(), list = products)
    }

    // the database might be missing certain products, so we need to fetch the ones we don't have
    private fun fetchOrderProductsAsync() = async {
        val productIds = order.items.map { it.productId }
        val numLocalProducts = orderDetailRepository.getProductCountForOrder(productIds)
        if (numLocalProducts != order.items.size) {
            orderDetailRepository.fetchProductsByRemoteIds(productIds)
        }
    }

    private fun fetchSLCreationEligibilityAsync() = async {
        if (isShippingPluginReady) {
            orderDetailRepository.fetchSLCreationEligibility(order.remoteId)
        }
    }

    private fun loadShipmentTracking(shippingLabels: ListInfo<ShippingLabel>): ListInfo<OrderShipmentTracking> {
        val trackingList = orderDetailRepository.getOrderShipmentTrackings(orderIdSet.id)
        return if (!appPrefs.isTrackingExtensionAvailable() || shippingLabels.isVisible || hasVirtualProductsOnly()) {
            ListInfo(isVisible = false)
        } else {
            ListInfo(list = trackingList)
        }
    }

    private fun fetchOrderRefundsAsync() = async {
        orderDetailRepository.fetchOrderRefunds(orderIdSet.remoteOrderId)
    }

    private fun fetchShipmentTrackingAsync() = async {
        val result = orderDetailRepository.fetchOrderShipmentTrackingList(orderIdSet.id, orderIdSet.remoteOrderId)
        appPrefs.setTrackingExtensionAvailable(result == SUCCESS)
    }

    private fun fetchOrderShippingLabelsAsync() = async {
        orderDetailRepository.fetchOrderShippingLabels(orderIdSet.remoteOrderId)
    }

    private fun loadOrderShippingLabels(): ListInfo<ShippingLabel> {
        orderDetailRepository.getOrderShippingLabels(orderIdSet.remoteOrderId)
            .loadProducts(order.items)
            .whenNotNullNorEmpty {
                return ListInfo(list = it)
            }
        return ListInfo(isVisible = false)
    }

    private suspend fun fetchProductAndShippingDetails() {
        awaitAll(
            fetchOrderShippingLabelsAsync(),
            fetchShipmentTrackingAsync(),
            fetchOrderRefundsAsync(),
            fetchOrderProductsAsync(),
            fetchSLCreationEligibilityAsync()
        )
    }

    private fun displayProductAndShippingDetails() {
        val shippingLabels = loadOrderShippingLabels()
        val shipmentTracking = loadShipmentTracking(shippingLabels)
        val orderRefunds = loadOrderRefunds()
        val orderProducts = loadOrderProducts(orderRefunds, shippingLabels)

        if (shippingLabels.isVisible) {
            _shippingLabels.value = shippingLabels.list
        }

        if (orderProducts.isVisible) {
            _productList.value = orderProducts.list
        }

        if (orderRefunds.isVisible) {
            _orderRefunds.value = orderRefunds.list
        }

        if (shipmentTracking.isVisible) {
            _shipmentTrackings.value = shipmentTracking.list
        }

        viewState = viewState.copy(
            isCreateShippingLabelButtonVisible = isShippingPluginReady &&
                orderDetailRepository.isOrderEligibleForSLCreation(order.remoteId),
            isShipmentTrackingAvailable = shipmentTracking.isVisible,
            isProductListVisible = orderProducts.isVisible,
            areShippingLabelsVisible = shippingLabels.isVisible
        )
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductImageChanged(event: OnProductImageChanged) {
        viewState = viewState.copy(refreshedProductId = event.remoteProductId)
    }

    @Parcelize
    data class ViewState(
        val order: Order? = null,
        val toolbarTitle: String? = null,
        val orderStatus: OrderStatus? = null,
        val isOrderDetailSkeletonShown: Boolean? = null,
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

    data class ListInfo<T>(val isVisible: Boolean = true, val list: List<T> = emptyList())

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<OrderDetailViewModel>
}
