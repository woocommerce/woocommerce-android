package com.woocommerce.android.ui.orders.details

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.IsScreenLargerThanCompactValue
import com.woocommerce.android.analytics.deviceTypeToAnalyticsString
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.model.GiftCardSummary
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.model.Subscription
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.model.loadProducts
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.ProductImageMap.OnProductFetchedListener
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.giftcard.GiftCardRepository
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderNote
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderShipmentTracking
import com.woocommerce.android.ui.orders.OrderNavigationTarget.EditOrder
import com.woocommerce.android.ui.orders.OrderNavigationTarget.IssueOrderRefund
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PreviewReceipt
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PrintShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.RefundShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartPaymentFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartShippingLabelCreationFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewCreateShippingLabelInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderFulfillInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderedAddons
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewPrintCustomsForm
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewPrintingInstructions
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewRefundedProducts
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.creation.shipping.GetShippingMethodsWithOtherValue
import com.woocommerce.android.ui.orders.creation.shipping.RefreshShippingMethods
import com.woocommerce.android.ui.orders.details.customfields.CustomOrderFieldsHelper
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.OrderAttributionInfo
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
@Suppress("LargeClass")
class OrderDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefs: AppPrefs,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val orderDetailRepository: OrderDetailRepository,
    private val addonsRepository: AddonRepository,
    private val selectedSite: SelectedSite,
    private val productImageMap: ProductImageMap,
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker,
    private val paymentsFlowTracker: PaymentsFlowTracker,
    private val tracker: OrderDetailTracker,
    private val shippingLabelOnboardingRepository: ShippingLabelOnboardingRepository,
    private val orderDetailsTransactionLauncher: OrderDetailsTransactionLauncher,
    private val getOrderSubscriptions: GetOrderSubscriptions,
    private val giftCardRepository: GiftCardRepository,
    private val orderProductMapper: OrderProductMapper,
    private val productDetailRepository: ProductDetailRepository,
    private val paymentReceiptHelper: PaymentReceiptHelper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val refreshShippingMethods: RefreshShippingMethods,
    getShippingMethodsWithOtherValue: GetShippingMethodsWithOtherValue
) : ScopedViewModel(savedState), OnProductFetchedListener {
    private val navArgs: OrderDetailFragmentArgs by savedState.navArgs()

    val performanceObserver: LifecycleObserver = orderDetailsTransactionLauncher

    var order: Order
        get() = requireNotNull(viewState.orderInfo?.order)
        set(value) {
            viewState = viewState.copy(
                orderInfo = viewState.orderInfo?.copy(
                    order = value,
                    isPaymentCollectableWithCardReader = viewState.orderInfo?.isPaymentCollectableWithCardReader
                        ?: false
                ) ?: OrderDetailViewState.OrderInfo(
                    value,
                    isPaymentCollectableWithCardReader = false
                )
            )
        }

    // Keep track of the deleted shipment tracking number in case
    // the request to server fails, we need to display an error message
    // and add the deleted tracking number back to the list
    private var deletedOrderShipmentTrackingSet = mutableSetOf<String>()

    val viewStateData = LiveDataDelegate(savedState, OrderDetailViewState())
    private var viewState by viewStateData

    private val _orderNotes = MutableLiveData<List<OrderNote>>()
    val orderNotes: LiveData<List<OrderNote>> = _orderNotes

    private val _orderRefunds = MutableLiveData<List<Refund>>()
    val orderRefunds: LiveData<List<Refund>> = _orderRefunds

    private val _productList = MutableLiveData<List<OrderProduct>>()
    val productList: LiveData<List<OrderProduct>> = _productList

    private val _feeLineList = MutableLiveData<List<Order.FeeLine>>()
    val feeLineList: LiveData<List<Order.FeeLine>> = _feeLineList

    private val _shipmentTrackings = MutableLiveData<List<OrderShipmentTracking>>()
    val shipmentTrackings: LiveData<List<OrderShipmentTracking>> = _shipmentTrackings

    private val _shippingLabels = MutableLiveData<List<ShippingLabel>>()
    val shippingLabels: LiveData<List<ShippingLabel>> = _shippingLabels

    private val _giftCards = MutableLiveData<List<GiftCardSummary>>()
    val giftCards: LiveData<List<GiftCardSummary>> = _giftCards

    private val _subscriptions = MutableLiveData<List<Subscription>>()
    val subscriptions: LiveData<List<Subscription>> = _subscriptions

    private val _orderAttributionInfo = MutableLiveData<OrderAttributionInfo>()
    val orderAttributionInfo: LiveData<OrderAttributionInfo> = _orderAttributionInfo

    private val _shippingLineList = MutableStateFlow<List<Order.ShippingLine>>(emptyList())
    val shippingLineList =
        combine(
            _shippingLineList.filter { it.isNotEmpty() },
            getShippingMethodsWithOtherValue().withIndex()
        ) { shippingLines, shippingMethods ->
            val shippingMethodsMap = shippingMethods.value.associateBy { it.id }
            var shouldRefreshShippingMethods = false
            val result = shippingLines.map { shippingLine ->
                val method = shippingLine.methodId?.let { shippingMethodsMap[it] }
                shouldRefreshShippingMethods = shouldRefreshShippingMethods ||
                    shippingLine.methodId.isNullOrEmpty().not() && method == null && shippingMethods.index == 0
                ShippingLineDetails(
                    id = shippingLine.itemId,
                    name = shippingLine.methodTitle,
                    shippingMethod = method,
                    amount = shippingLine.total
                )
            }
            if (shouldRefreshShippingMethods) launch { refreshShippingMethods() }
            result
        }.asLiveData()

    private var isFetchingData = false

    private val productListObserver = Observer<List<OrderProduct>> { products ->
        launch {
            trackProductsLoaded(products)
        }
    }

    override fun onCleared() {
        super.onCleared()
        productImageMap.unsubscribeFromOnProductFetchedEvents(this)
        orderDetailsTransactionLauncher.clear()
        _productList.removeObserver(productListObserver)
    }

    private var pluginsInformation: Map<String, WooPlugin> = HashMap()

    init {
        productImageMap.subscribeToOnProductFetchedEvents(this)
        launch {
            pluginsInformation = orderDetailRepository.getOrderDetailsPluginsInfo()
        }
        _productList.distinctUntilChanged().observeForever(productListObserver)

        if (navArgs.startPaymentFlow) {
            triggerEvent(
                StartPaymentFlow(
                    orderId = navArgs.orderId,
                    paymentTypeFlow = CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER_CREATION
                )
            )
        }
    }

    fun start() {
        if (navArgs.orderId != -1L) {
            launch {
                orderDetailRepository.getOrderById(navArgs.orderId)?.let {
                    order = it
                    displayOrderDetails()
                    fetchOrder(showSkeleton = false)
                } ?: fetchOrder(showSkeleton = true)
            }
        } else {
            viewState = viewState.copy(isOrderDetailSkeletonShown = true)
        }
    }

    fun hasOrder() = viewState.orderInfo?.order != null

    private suspend fun displayOrderDetails() {
        updateOrderState()
        loadOrderNotes()
        displayProductAndShippingDetails()
        displayCustomAmounts()
        checkOrderMetaData()
    }

    private suspend fun fetchOrder(showSkeleton: Boolean) {
        // Prevent re-fetch data when a fetching request is ongoing
        if (isFetchingData) return

        if (networkStatus.isConnected()) {
            viewState = viewState.copy(
                isOrderDetailSkeletonShown = showSkeleton
            )

            isFetchingData = true
            awaitAll(
                fetchOrderAsync(),
                fetchOrderNotesAsync(),
                fetchOrderShippingLabelsAsync(),
                fetchShipmentTrackingAsync(),
                fetchOrderRefundsAsync(),
                fetchSLCreationEligibilityAsync(),
            )
            isFetchingData = false

            if (hasOrder()) {
                displayOrderDetails()
                awaitAll(
                    fetchOrderSubscriptionsAsync(),
                    fetchGiftCardsAsync()
                )
            }

            viewState = viewState.copy(
                isOrderDetailSkeletonShown = false,
                isRefreshing = false
            )
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
            viewState = viewState.copy(
                isOrderDetailSkeletonShown = false,
                isRefreshing = false
            )
        }
    }

    private suspend fun checkOrderMetaData() {
        viewState = viewState.copy(
            isCustomFieldsButtonShown = orderDetailRepository.orderHasMetadata(navArgs.orderId)
        )
    }

    /**
     * User clicked the button to view custom fields
     */
    fun onCustomFieldsButtonClicked() {
        tracker.trackCustomFieldsTapped()
        triggerEvent(OrderNavigationTarget.ViewCustomFields(navArgs.orderId))
    }

    /**
     * User tapped an actionable custom field
     */
    fun onCustomFieldClicked(context: Context, value: String) {
        CustomOrderFieldsHelper.handleMetadataValue(context, value)
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun getOrderMetadata(): List<OrderMetaDataEntity> = runBlocking {
        orderDetailRepository.getOrderMetadata(navArgs.orderId)
    }

    fun onRefreshRequested() {
        tracker.trackOrderDetailPulledToRefresh()
        viewState = viewState.copy(isRefreshing = true)
        launch { fetchOrder(false) }
    }

    fun hasVirtualProductsOnly(): Boolean {
        return if (order.items.isNotEmpty()) {
            val remoteProductIds = order.getProductIds()
            orderDetailRepository.hasVirtualProductsOnly(remoteProductIds)
        } else {
            false
        }
    }

    fun onEditOrderStatusSelected() {
        viewState.orderStatus?.let { orderStatus ->
            triggerEvent(
                ViewOrderStatusSelector(
                    currentStatus = orderStatus.statusKey,
                    orderStatusList = orderDetailRepository.getOrderStatusOptions().toTypedArray()
                )
            )
        }
    }

    fun onIssueOrderRefundClicked() {
        triggerEvent(IssueOrderRefund(remoteOrderId = order.id))
    }

    fun onEditClicked() {
        tracker.trackEditButtonTapped(order.feesLines.size, order.shippingLines.size)
        val firstGiftCard = giftCards.value?.firstOrNull()
        triggerEvent(
            EditOrder(
                orderId = order.id,
                giftCard = firstGiftCard?.code,
                appliedDiscount = firstGiftCard?.used
            )
        )
    }

    fun orderNavigationIsEnabled() = navArgs.allOrderIds?.let {
        it.contains(navArgs.orderId) && it.count() > 1
    } ?: false

    fun onPreviousOrderClicked() {
        if (!previousOrderNavigationIsEnabled()) return

        navArgs.allOrderIds?.let {
            val previousIndex = it.indexOf(navArgs.orderId) - 1
            val previousOrderId = it.get(previousIndex)
            triggerEvent(OrderNavigationTarget.ShowOrder(previousOrderId, it))
        }
    }

    fun previousOrderNavigationIsEnabled() = navArgs.allOrderIds?.let {
        it.contains(navArgs.orderId) && it.first() != navArgs.orderId
    } ?: false

    fun onNextOrderClicked() {
        if (!nextOrderNavigationIsEnabled()) return

        navArgs.allOrderIds?.let {
            val nextIndex = it.indexOf(navArgs.orderId) + 1
            val nextOrderId = it.get(nextIndex)
            triggerEvent(OrderNavigationTarget.ShowOrder(nextOrderId, it))
        }
    }

    fun nextOrderNavigationIsEnabled() = navArgs.allOrderIds?.let {
        it.contains(navArgs.orderId) && it.last() != navArgs.orderId
    } ?: false

    fun onCollectPaymentClicked(isTablet: Boolean = false) {
        paymentsFlowTracker.trackCollectPaymentTapped(
            IsScreenLargerThanCompactValue(isTablet).deviceTypeToAnalyticsString
        )
        triggerEvent(
            StartPaymentFlow(
                orderId = navArgs.orderId,
                paymentTypeFlow = CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
            )
        )
    }

    fun onSeeReceiptClicked() {
        launch {
            tracker.trackReceiptViewTapped(order.id, order.status)

            viewState = viewState.copy(
                orderInfo = viewState.orderInfo?.copy(
                    receiptButtonStatus = OrderDetailViewState.ReceiptButtonStatus.Loading
                )
            )

            val receiptResult = paymentReceiptHelper.getReceiptUrl(order.id)

            viewState = viewState.copy(
                orderInfo = viewState.orderInfo?.copy(
                    receiptButtonStatus = OrderDetailViewState.ReceiptButtonStatus.Visible
                )
            )

            if (receiptResult.isSuccess) {
                triggerEvent(PreviewReceipt(order.billingAddress.email, receiptResult.getOrThrow(), order.id))
            } else {
                paymentsFlowTracker.trackReceiptUrlFetchingFails(
                    errorDescription = receiptResult.exceptionOrNull()?.message ?: "Unknown error",
                )
                triggerEvent(ShowSnackbar(string.receipt_fetching_error))
            }
        }
    }

    fun onPrintingInstructionsClicked() {
        triggerEvent(ViewPrintingInstructions)
    }

    fun onGetWcShippingClicked() {
        triggerEvent(InstallWCShippingViewModel.InstallWcShipping)
    }

    /**
     * This is triggered when the user taps "Done" on any of the order editing fragments
     */
    fun onOrderEdited() {
        reloadOrderDetails()
    }

    /**
     * This is triggered when the above network request to edit an order fails
     */
    fun onOrderEditFailed(@StringRes message: Int) {
        reloadOrderDetails()
        triggerEvent(ShowSnackbar(message))
    }

    fun onViewRefundedProductsClicked() {
        triggerEvent(ViewRefundedProducts(orderId = order.id))
    }

    fun onAddOrderNoteClicked() {
        triggerEvent(AddOrderNote(orderId = order.id, orderNumber = order.number))
    }

    fun onRefundShippingLabelClick(shippingLabelId: Long) {
        triggerEvent(RefundShippingLabel(remoteOrderId = order.id, shippingLabelId = shippingLabelId))
    }

    fun onPrintShippingLabelClicked(shippingLabelId: Long) {
        triggerEvent(PrintShippingLabel(remoteOrderId = order.id, shippingLabelId = shippingLabelId))
    }

    fun onPrintCustomsFormClicked(shippingLabel: ShippingLabel) {
        shippingLabel.commercialInvoiceUrl?.let {
            triggerEvent(ViewPrintCustomsForm(listOf(it), isReprint = true))
        }
    }

    fun onAddShipmentTrackingClicked() {
        triggerEvent(
            AddOrderShipmentTracking(
                orderId = order.id,
                orderTrackingProvider = appPrefs.getSelectedShipmentTrackingProviderName(),
                isCustomProvider = appPrefs.getIsSelectedShipmentTrackingProviderCustom()
            )
        )
    }

    fun onNewShipmentTrackingAdded(shipmentTracking: OrderShipmentTracking) {
        tracker.trackAddOrderTrackingTapped(order.id, order.status, shipmentTracking.trackingProvider)
        refreshShipmentTracking()
    }

    fun refreshShipmentTracking() {
        _shipmentTrackings.value = orderDetailRepository.getOrderShipmentTrackings(order.id)
    }

    fun onShippingLabelRefunded() {
        launch {
            fetchOrderShippingLabelsAsync().await()
            displayOrderDetails()
        }
    }

    fun onShippingLabelsPurchased() {
        launch {
            // Refresh UI from the database, as new labels are cached by FluxC after the purchase,
            // if for any reason, the order wasn't found, refetch it
            orderDetailRepository.getOrderById(navArgs.orderId)?.let {
                order = it
                displayOrderDetails()
            } ?: fetchOrder(true)
        }
    }

    fun onOrderItemRefunded() {
        launch { fetchOrder(false) }
    }

    fun onOrderStatusChanged(updateSource: OrderStatusUpdateSource) {
        tracker.trackOrderStatusChanged(order.id, order.status.value, updateSource.newStatus)

        val snackbarMessage = when (updateSource) {
            is OrderStatusUpdateSource.FullFillScreen -> string.order_fulfill_completed
            else -> string.order_status_updated
        }

        triggerEvent(
            ShowUndoSnackbar(
                message = resourceProvider.getString(snackbarMessage),
                undoAction = {
                    updateOrderStatus(updateSource.oldStatus)
                },
            )
        )

        updateOrderStatus(updateSource.newStatus)
    }

    fun onNewOrderNoteAdded(orderNote: OrderNote) {
        val orderNotes = _orderNotes.value?.toMutableList() ?: mutableListOf()
        orderNotes.add(0, orderNote)
        _orderNotes.value = orderNotes
    }

    fun onDeleteShipmentTrackingClicked(trackingNumber: String) {
        if (networkStatus.isConnected()) {
            orderDetailRepository.getOrderShipmentTrackingByTrackingNumber(
                order.id,
                trackingNumber
            )?.let { deletedShipmentTracking ->
                deletedOrderShipmentTrackingSet.add(trackingNumber)

                val shipmentTrackings = _shipmentTrackings.value?.toMutableList() ?: mutableListOf()
                shipmentTrackings.remove(deletedShipmentTracking)
                _shipmentTrackings.value = shipmentTrackings

                triggerEvent(
                    ShowUndoSnackbar(
                        message = resourceProvider.getString(string.order_shipment_tracking_delete_snackbar_msg),
                        undoAction = { onDeleteShipmentTrackingReverted(deletedShipmentTracking) },
                        dismissAction = object : Snackbar.Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                if (event != DISMISS_EVENT_ACTION) {
                                    // delete the shipment only if user has not clicked on the undo snackbar
                                    deleteOrderShipmentTracking(deletedShipmentTracking)
                                }
                            }
                        }
                    )
                )
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
            val onOrderChanged = orderDetailRepository.deleteOrderShipmentTracking(
                navArgs.orderId,
                shipmentTracking.toDataModel()
            )
            if (!onOrderChanged.isError) {
                tracker.trackOrderTrackingDeleteSucceeded()
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_delete_success))
            } else {
                tracker.trackOrderTrackingDeleteFailed(onOrderChanged.error)
                onDeleteShipmentTrackingReverted(shipmentTracking)
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_delete_error))
            }
        }
    }

    private fun updateOrderStatus(newStatus: String) {
        if (networkStatus.isConnected()) {
            launch {
                orderDetailRepository.updateOrderStatus(order.id, newStatus)
                    .collect { result ->
                        reloadOrderDetails()
                        when (result) {
                            is OptimisticUpdateResult -> {
                                // no-op. We reload order details in any case
                            }

                            is RemoteUpdateResult -> {
                                if (result.event.isError) {
                                    triggerEvent(ShowSnackbar(string.order_error_update_general))
                                    tracker.trackOrderStatusChangeFailed(result.event.error)
                                } else {
                                    tracker.trackOrderStatusChangeSucceeded()
                                }
                            }
                        }
                    }
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    fun onShippingLabelNoticeTapped() {
        triggerEvent(ViewCreateShippingLabelInfo)
    }

    fun onCreateShippingLabelButtonTapped() {
        tracker.trackShippinhLabelTapped()
        triggerEvent(StartShippingLabelCreationFlow(order.id))
    }

    fun onMarkOrderCompleteButtonTapped() {
        tracker.trackMarkOrderAsCompleteTapped()
        triggerEvent(ViewOrderFulfillInfo(order.id))
    }

    fun onViewOrderedAddonButtonTapped(orderItem: Order.Item) {
        tracker.trackViewAddonsTapped()
        triggerEvent(
            ViewOrderedAddons(
                navArgs.orderId,
                orderItem.itemId,
                orderItem.productId
            )
        )
    }

    fun onTrashOrderClicked() {
        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                messageId = string.order_detail_trash_order_dialog_message,
                positiveButtonId = string.order_detail_move_to_trash,
                positiveBtnAction = { _, _ ->
                    analyticsTracker.track(AnalyticsEvent.ORDER_DETAIL_TRASH_TAPPED)
                    triggerEvent(TrashOrder(navArgs.orderId))
                },
                negativeButtonId = string.cancel
            )
        )
    }

    private suspend fun updateOrderState() {
        val isPaymentCollectable = isPaymentCollectable(order)
        val orderStatus = orderDetailRepository.getOrderStatus(order.status.value)
        viewState = viewState.copy(
            orderInfo = OrderDetailViewState.OrderInfo(
                order = order,
                isPaymentCollectableWithCardReader = isPaymentCollectable,
                receiptButtonStatus = if (paymentReceiptHelper.isReceiptAvailable(order.id) && order.isOrderPaid) {
                    OrderDetailViewState.ReceiptButtonStatus.Visible
                } else {
                    OrderDetailViewState.ReceiptButtonStatus.Hidden
                }
            ),
            orderStatus = orderStatus,
            toolbarTitle = resourceProvider.getString(
                string.orderdetail_orderstatus_ordernum, order.number
            ),
        )
    }

    private suspend fun isPaymentCollectable(order: Order) = paymentCollectibilityChecker.isCollectable(order)

    private fun loadOrderNotes() {
        launch {
            _orderNotes.value = orderDetailRepository.getOrderNotes(navArgs.orderId)
        }
    }

    private fun fetchOrderAsync() = async {
        val fetchedOrder = orderDetailRepository.fetchOrderById(navArgs.orderId)
        orderDetailsTransactionLauncher.onOrderFetched()
        if (fetchedOrder != null) {
            order = fetchedOrder
            fetchOrderProducts()
        } else {
            triggerEvent(ShowSnackbar(string.order_error_fetch_generic))
        }
    }

    private fun fetchOrderNotesAsync() = async {
        if (!orderDetailRepository.fetchOrderNotes(navArgs.orderId)) {
            triggerEvent(ShowSnackbar(string.order_error_fetch_notes_generic))
        }
        orderDetailsTransactionLauncher.onNotesFetched()
    }

    private fun loadOrderRefunds(): ListInfo<Refund> {
        return ListInfo(list = orderDetailRepository.getOrderRefunds(navArgs.orderId))
    }

    private suspend fun loadOrderProducts(
        refunds: ListInfo<Refund>
    ): ListInfo<OrderProduct> {
        val products = refunds.list.getNonRefundedProducts(order.items)
        checkAddonAvailability(products)
        val orderProducts = orderProductMapper.toOrderProducts(_productList.value ?: emptyList(), products)
        return ListInfo(isVisible = orderProducts.isNotEmpty(), list = orderProducts)
    }

    private suspend fun trackProductsLoaded(orderProducts: List<OrderProduct>) {
        if (orderProducts.isEmpty()) return
        val ids = orderProducts.map { orderProduct -> orderProduct.product.productId }
        val productTypes = orderDetailRepository.getUniqueProductTypes(ids)
        val hasAddons = orderProducts.any { orderProduct -> orderProduct.product.containsAddons }
        tracker.trackProductsLoaded(order.id, productTypes, hasAddons)
    }

    private suspend fun checkAddonAvailability(products: List<Order.Item>) {
        products.forEach { it.containsAddons = addonsRepository.containsAddonsFrom(it) }
    }

    // the database might be missing certain products, so we need to fetch the ones we don't have
    private suspend fun fetchOrderProducts() {
        val productIds = order.getProductIds()
        val numLocalProducts = orderDetailRepository.getProductCountForOrder(productIds)
        if (numLocalProducts != order.items.size) {
            orderDetailRepository.fetchProductsByRemoteIds(productIds)
        }
    }

    private fun fetchSLCreationEligibilityAsync() = async {
        if (shippingLabelOnboardingRepository.isShippingPluginReady) {
            orderDetailRepository.fetchSLCreationEligibility(navArgs.orderId)
        }
        orderDetailsTransactionLauncher.onPackageCreationEligibleFetched()
    }

    private fun loadShipmentTracking(shippingLabels: ListInfo<ShippingLabel>): ListInfo<OrderShipmentTracking> {
        val trackingList = orderDetailRepository.getOrderShipmentTrackings(navArgs.orderId)
        return if (!appPrefs.isTrackingExtensionAvailable() || shippingLabels.isVisible || hasVirtualProductsOnly()) {
            ListInfo(isVisible = false)
        } else {
            ListInfo(list = trackingList)
        }
    }

    private fun fetchOrderRefundsAsync() = async {
        orderDetailRepository.fetchOrderRefunds(navArgs.orderId)
        orderDetailsTransactionLauncher.onRefundsFetched()
    }

    private fun fetchShipmentTrackingAsync() = async {
        val plugin = pluginsInformation[WooCommerceStore.WooPlugin.WOO_SHIPMENT_TRACKING.pluginName]

        if (plugin == null || plugin.isOperational) {
            val result = orderDetailRepository.fetchOrderShipmentTrackingList(navArgs.orderId)
            appPrefs.setTrackingExtensionAvailable(result == SUCCESS)
        }

        orderDetailsTransactionLauncher.onShipmentTrackingFetchingCompleted()
    }

    private fun fetchOrderShippingLabelsAsync() = async {
        if (shippingLabelOnboardingRepository.isShippingPluginReady) {
            orderDetailRepository.fetchOrderShippingLabels(navArgs.orderId)
        }
        orderDetailsTransactionLauncher.onShippingLabelFetchingCompleted()
    }

    private fun fetchOrderSubscriptionsAsync() = async {
        val plugin = pluginsInformation[WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName]
        if (plugin != null && plugin.isOperational) {
            getOrderSubscriptions(navArgs.orderId).getOrNull()?.let { subscription ->
                _subscriptions.value = subscription
                if (subscription.isNotEmpty()) {
                    tracker.trackOrderDetailsSubscriptionsShown()
                }
            }
        }
        orderDetailsTransactionLauncher.onSubscriptionsFetched()
    }

    private suspend fun fetchGiftCardsAsync() = async {
        val plugin = pluginsInformation[WooCommerceStore.WooPlugin.WOO_GIFT_CARDS.pluginName]
        if (plugin != null && plugin.isOperational) {
            giftCardRepository.fetchGiftCardSummaryByOrderId(navArgs.orderId)
                .takeIf { result -> result.isError.not() }
                ?.let { result ->
                    val giftCardSummaries = result.model ?: return@let
                    _giftCards.value = giftCardSummaries
                    if (giftCardSummaries.isNotEmpty()) {
                        tracker.trackOrderDetailsGiftCardShown()
                    }
                }
        }
        orderDetailsTransactionLauncher.onGiftCardsFetched()
    }

    private fun loadOrderShippingLabels(): ListInfo<ShippingLabel> {
        orderDetailRepository.getOrderShippingLabels(navArgs.orderId)
            .loadProducts(order.items)
            .whenNotNullNorEmpty {
                return ListInfo(list = it)
            }
        return ListInfo(isVisible = false)
    }

    private suspend fun displayProductAndShippingDetails() {
        val shippingLabels = loadOrderShippingLabels()
        val shipmentTracking = loadShipmentTracking(shippingLabels)
        val orderRefunds = loadOrderRefunds()
        val orderProducts = loadOrderProducts(orderRefunds)

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

        _shippingLineList.value = order.shippingLines

        _orderAttributionInfo.value = orderDetailRepository.getOrderAttributionInfo(navArgs.orderId)

        val orderEligibleForInPersonPayments = viewState.orderInfo?.isPaymentCollectableWithCardReader == true

        val isOrderEligibleForSLCreation = shippingLabelOnboardingRepository.isShippingPluginReady &&
            orderDetailRepository.isOrderEligibleForSLCreation(order.id) &&
            !orderEligibleForInPersonPayments

        if (isOrderEligibleForSLCreation &&
            viewState.isCreateShippingLabelButtonVisible != true &&
            viewState.isProductListMenuVisible != true
        ) {
            // we check against the viewstate to avoid sending the event multiple times
            // if the eligibility was cached, and we had the same value after re-fetching it
            tracker.trackOrderEligibleForShippingLabelCreation(order.status.value)
        }

        viewState = viewState.copy(
            isCreateShippingLabelButtonVisible = isOrderEligibleForSLCreation && !shippingLabels.isVisible,
            isProductListMenuVisible = isOrderEligibleForSLCreation && shippingLabels.isVisible,
            isShipmentTrackingAvailable = shipmentTracking.isVisible,
            isProductListVisible = orderProducts.isVisible,
            areShippingLabelsVisible = shippingLabels.isVisible,
            wcShippingBannerVisible = shippingLabelOnboardingRepository.shouldShowWcShippingBanner(
                order,
                orderEligibleForInPersonPayments
            ),
            isAIThankYouNoteButtonShown = shouldShowThankYouNoteButton()
        )
    }

    private fun shouldShowThankYouNoteButton() =
        selectedSite.getIfExists()?.isWPComAtomic == true &&
            order.status == Order.Status.Completed &&
            productList.value?.isNotEmpty() == true

    private fun displayCustomAmounts() {
        _feeLineList.value = order.feesLines
    }

    override fun onProductFetched(remoteProductId: Long) {
        viewState = viewState.copy(refreshedProductId = remoteProductId)
    }

    fun onCardReaderPaymentCompleted() {
        reloadOrderDetails()
    }

    private fun reloadOrderDetails() {
        launch {
            orderDetailRepository.getOrderById(navArgs.orderId)?.let {
                order = it
            } ?: WooLog.w(T.ORDERS, "Order ${navArgs.orderId} not found in the database.")
            displayOrderDetails()
        }
    }

    fun onWcShippingBannerDismissed() {
        shippingLabelOnboardingRepository.markWcShippingBannerAsDismissed()
    }

    fun onAIThankYouNoteButtonClicked() {
        launch {
            val orderRefunds = loadOrderRefunds()
            val orderProducts = loadOrderProducts(orderRefunds)

            val firstProductId = when (val first = orderProducts.list.first()) {
                is OrderProduct.GroupedProductItem -> first.product.productId
                is OrderProduct.ProductItem -> first.product.productId
            }

            val product = productDetailRepository.getProductAsync(firstProductId)
            product?.let {
                triggerEvent(
                    OrderNavigationTarget.AIThankYouNote(
                        customerName = order.billingAddress.firstName,
                        productName = it.name,
                        productDescription = it.description
                    )
                )
            }
        }
    }

    fun showEmptyView() {
        viewState = viewState.copy(
            isOrderDetailEmpty = true,
            isRefreshing = false,
            isOrderDetailSkeletonShown = false
        )
    }

    fun showLoadingView() {
        viewState = viewState.copy(
            isOrderDetailEmpty = false,
            isOrderDetailSkeletonShown = true
        )
    }

    data class ListInfo<T>(val isVisible: Boolean = true, val list: List<T> = emptyList())
    data class TrashOrder(val orderId: Long) : MultiLiveEvent.Event()

    data class ShippingLineDetails(
        val id: Long,
        val shippingMethod: ShippingMethod?,
        val amount: BigDecimal,
        val name: String
    )
}
