package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R.string
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CUSTOMER_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_FEE_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_FEE_REMOVE
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_NOTE_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_PRODUCT_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_PRODUCT_QUANTITY_CHANGE
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_PRODUCT_REMOVE
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_SHIPPING_METHOD_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_SHIPPING_METHOD_REMOVE
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_STATUS_CHANGE
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FROM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_CUSTOMER_DETAILS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_DIFFERENT_SHIPPING_DETAILS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_FEES
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_SHIPPING_METHOD
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PARENT_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATUS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_TO
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.OrderNoteType.CUSTOMER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_CREATION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_EDITING
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.Order.ShippingLine
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.AddProduct
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditFee
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditShipping
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowProductDetails
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderCreateEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val mapItemToProductUiModel: MapItemToProductUiModel,
    private val createOrderItem: CreateOrderItem,
    private val determineMultipleLinesContext: DetermineMultipleLinesContext,
    private val tracker: AnalyticsTrackerWrapper,
    autoSyncOrder: AutoSyncOrder,
    autoSyncPriceModifier: AutoSyncPriceModifier,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val PARAMETERS_KEY = "parameters_key"
        private const val ORDER_CUSTOM_FEE_NAME = "order_custom_fee"
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val args: OrderCreateEditFormFragmentArgs by savedState.navArgs()
    val mode: Mode = args.mode

    private val flow = when (mode) {
        Mode.Creation -> VALUE_FLOW_CREATION
        is Mode.Edit -> VALUE_FLOW_EDITING
    }

    private val _orderDraft = savedState.getStateFlow(viewModelScope, Order.EMPTY)
    val orderDraft = _orderDraft
        .asLiveData()

    val orderStatusData: LiveData<OrderStatus> = _orderDraft
        .map { it.status }
        .distinctUntilChanged()
        .map { status ->
            withContext(dispatchers.io) {
                orderDetailRepository.getOrderStatus(status.value)
            }
        }.asLiveData()

    val products: LiveData<List<ProductUIModel>> = _orderDraft
        .map { order -> order.items.filter { it.quantity > 0 } }
        .distinctUntilChanged()
        .map { items ->
            items.map { item -> mapItemToProductUiModel(item) }
        }.asLiveData()

    private val retryOrderDraftUpdateTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val syncStrategy =
        when (mode) {
            Mode.Creation -> autoSyncPriceModifier
            is Mode.Edit -> autoSyncOrder
        }

    fun getProductUIModelFromItem(item: Order.Item) = runBlocking {
        mapItemToProductUiModel(item)
    }

    val currentDraft
        get() = _orderDraft.value

    private val orderCreationStatus = Order.Status.Custom(Order.Status.AUTO_DRAFT)

    init {
        when (mode) {
            Mode.Creation -> {
                _orderDraft.update {
                    it.copy(
                        currency = parameterRepository.getParameters(
                            PARAMETERS_KEY,
                            savedState
                        ).currencyCode.orEmpty()
                    )
                }
                monitorOrderChanges()
            }
            is Mode.Edit -> {
                viewModelScope.launch {
                    orderDetailRepository.getOrderById(mode.orderId)?.let { order ->
                        _orderDraft.value = order
                        viewState = viewState.copy(
                            isUpdatingOrderDraft = false,
                            showOrderUpdateSnackbar = false,
                            isEditable = order.isEditable,
                            multipleLinesContext = determineMultipleLinesContext(order)
                        )
                        monitorOrderChanges()
                    }
                }
            }
        }
    }

    fun onCustomerNoteEdited(newNote: String) {
        _orderDraft.value.let { order ->
            tracker.track(
                ORDER_NOTE_ADD,
                mapOf(
                    KEY_PARENT_ID to order.id,
                    KEY_STATUS to order.status,
                    KEY_TYPE to CUSTOMER,
                    KEY_FLOW to flow,
                )
            )
        }
        _orderDraft.update { it.copy(customerNote = newNote) }
    }

    fun onIncreaseProductsQuantity(id: Long) {
        tracker.track(
            ORDER_PRODUCT_QUANTITY_CHANGE,
            mapOf(KEY_FLOW to flow)
        )
        _orderDraft.update { it.adjustProductQuantity(id, +1) }
    }

    fun onDecreaseProductsQuantity(id: Long) {
        _orderDraft.value.items
            .find { it.itemId == id }
            ?.takeIf { it.quantity == 1F }
            ?.let { onProductClicked(it) }
            ?: run {
                tracker.track(
                    ORDER_PRODUCT_QUANTITY_CHANGE,
                    mapOf(KEY_FLOW to flow)
                )
                _orderDraft.update { it.adjustProductQuantity(id, -1) }
            }
    }

    fun onOrderStatusChanged(status: Order.Status) {
        tracker.track(
            ORDER_STATUS_CHANGE,
            mapOf(
                KEY_ID to _orderDraft.value.id,
                KEY_FROM to _orderDraft.value.status.value,
                KEY_TO to status.value,
                KEY_FLOW to flow
            )
        )
        _orderDraft.update { it.copy(status = status) }
    }

    fun onRemoveProduct(item: Order.Item) = _orderDraft.update {
        tracker.track(
            ORDER_PRODUCT_REMOVE,
            mapOf(KEY_FLOW to flow)
        )
        it.adjustProductQuantity(item.itemId, -item.quantity.toInt())
    }

    fun onProductSelected(remoteProductId: Long, variationId: Long? = null) {
        tracker.track(
            ORDER_PRODUCT_ADD,
            mapOf(KEY_FLOW to flow)
        )

        viewModelScope.launch {
            _orderDraft.value.items.toMutableList().apply {
                add(createOrderItem(remoteProductId, variationId))
            }.let { items -> _orderDraft.update { it.updateItems(items) } }
        }
    }

    fun onCustomerAddressEdited(billingAddress: Address, shippingAddress: Address) {
        val hasDifferentShippingDetails = _orderDraft.value.shippingAddress != _orderDraft.value.billingAddress
        tracker.track(
            ORDER_CUSTOMER_ADD,
            mapOf(
                KEY_FLOW to flow,
                KEY_HAS_DIFFERENT_SHIPPING_DETAILS to hasDifferentShippingDetails
            )
        )

        _orderDraft.update { order ->
            order.copy(
                billingAddress = billingAddress,
                shippingAddress = shippingAddress.takeIf { it != Address.EMPTY } ?: billingAddress
            )
        }
    }

    fun onEditOrderStatusClicked(currentStatus: OrderStatus) {
        launch(dispatchers.io) {
            orderDetailRepository
                .getOrderStatusOptions().toTypedArray()
                .runWithContext(dispatchers.main) {
                    triggerEvent(
                        ViewOrderStatusSelector(
                            currentStatus = currentStatus.statusKey,
                            orderStatusList = it
                        )
                    )
                }
        }
    }

    fun onCustomerClicked() {
        triggerEvent(EditCustomer)
    }

    fun onCustomerNoteClicked() {
        triggerEvent(EditCustomerNote)
    }

    fun onAddProductClicked() {
        triggerEvent(AddProduct)
    }

    fun onProductClicked(item: Order.Item) {
        // Don't show details if the product is not synced yet
        if (!item.isSynced()) return
        triggerEvent(ShowProductDetails(item))
    }

    fun onRetryPaymentsClicked() {
        retryOrderDraftUpdateTrigger.tryEmit(Unit)
    }

    fun onFeeButtonClicked() {
        val order = _orderDraft.value
        val currentFee = order.feesLines.firstOrNull()

        val currentFeeValue = currentFee?.total
        val currentFeeTotalValue = currentFee?.getTotalValue() ?: BigDecimal.ZERO

        val orderSubtotal = order.total - currentFeeTotalValue
        triggerEvent(EditFee(orderSubtotal, currentFeeValue))
    }

    fun onShippingButtonClicked() {
        triggerEvent(EditShipping(currentDraft.shippingLines.firstOrNull { it.methodId != null }))
    }

    fun onCreateOrderClicked(order: Order) {
        when (mode) {
            Mode.Creation -> viewModelScope.launch {
                trackCreateOrderButtonClick()
                viewState = viewState.copy(isProgressDialogShown = true)
                orderCreateEditRepository.placeOrder(order).fold(
                    onSuccess = {
                        AnalyticsTracker.track(ORDER_CREATION_SUCCESS)
                        triggerEvent(ShowSnackbar(string.order_creation_success_snackbar))
                        triggerEvent(ShowCreatedOrder(it.id))
                    },
                    onFailure = {
                        trackOrderCreationFailure(it)
                        viewState = viewState.copy(isProgressDialogShown = false)
                        triggerEvent(ShowSnackbar(string.order_creation_failure_snackbar))
                    }
                )
            }
            is Mode.Edit -> {
                triggerEvent(Exit)
            }
        }
    }

    fun onBackButtonClicked() {
        when (mode) {
            Mode.Creation -> {
                if (_orderDraft.value.isEmpty()) {
                    triggerEvent(Exit)
                } else {
                    triggerEvent(
                        ShowDialog.buildDiscardDialogEvent(
                            positiveBtnAction = { _, _ ->
                                val draft = _orderDraft.value
                                if (draft.id != 0L) {
                                    launch { orderCreateEditRepository.deleteDraftOrder(draft) }
                                }
                                triggerEvent(Exit)
                            }
                        )
                    )
                }
            }
            is Mode.Edit -> {
                triggerEvent(Exit)
            }
        }
    }

    /**
     * Monitor order changes, and update the remote draft to update price totals
     */
    private fun monitorOrderChanges() {
        viewModelScope.launch {
            val changes =
                if (mode is Mode.Edit) {
                    _orderDraft.drop(1)
                } else {
                    // When we are in the order creation flow, we need to keep the order status as auto-draft.
                    // In this way, when the draft of the created order needs to synchronize the price modifiers,
                    // the application does not send notifications or synchronize its status on other devices.
                    _orderDraft.map { order -> order.copy(status = orderCreationStatus) }
                }
            syncStrategy.syncOrderChanges(changes, retryOrderDraftUpdateTrigger)
                .collect { updateStatus ->
                    when (updateStatus) {
                        OrderUpdateStatus.PendingDebounce ->
                            viewState = viewState.copy(willUpdateOrderDraft = true, showOrderUpdateSnackbar = false)
                        OrderUpdateStatus.Ongoing ->
                            viewState = viewState.copy(willUpdateOrderDraft = false, isUpdatingOrderDraft = true)
                        is OrderUpdateStatus.Failed -> {
                            trackOrderSyncFailed(updateStatus.throwable)
                            viewState = viewState.copy(isUpdatingOrderDraft = false, showOrderUpdateSnackbar = true)
                        }
                        is OrderUpdateStatus.Succeeded -> {
                            viewState = viewState.copy(
                                isUpdatingOrderDraft = false,
                                showOrderUpdateSnackbar = false,
                                isEditable = updateStatus.order.isEditable || mode is Mode.Creation,
                                multipleLinesContext = determineMultipleLinesContext(updateStatus.order)
                            )
                            _orderDraft.update { currentDraft ->
                                if (mode is Mode.Creation) {
                                    // Once the order is synced, revert the auto-draft status and keep
                                    // the user's selected one
                                    updateStatus.order.copy(status = currentDraft.status)
                                } else {
                                    updateStatus.order
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun trackOrderCreationFailure(it: Throwable) {
        tracker.track(
            ORDER_CREATION_FAILED,
            mapOf(
                KEY_ERROR_CONTEXT to this::class.java.simpleName,
                KEY_ERROR_TYPE to (it as? WooException)?.error?.type?.name,
                KEY_ERROR_DESC to it.message
            )
        )
    }

    private fun trackCreateOrderButtonClick() {
        tracker.track(
            ORDER_CREATE_BUTTON_TAPPED,
            mapOf(
                KEY_STATUS to _orderDraft.value.status,
                KEY_PRODUCT_COUNT to products.value?.count(),
                KEY_HAS_CUSTOMER_DETAILS to _orderDraft.value.billingAddress.hasInfo(),
                KEY_HAS_FEES to _orderDraft.value.feesLines.isNotEmpty(),
                KEY_HAS_SHIPPING_METHOD to _orderDraft.value.shippingLines.isNotEmpty()
            )
        )
    }

    private fun trackOrderSyncFailed(throwable: Throwable) {
        tracker.track(
            stat = AnalyticsEvent.ORDER_SYNC_FAILED,
            properties = mapOf(KEY_FLOW to flow),
            errorContext = this::class.java.simpleName,
            errorType = (throwable as? WooException)?.error?.type?.name,
            errorDescription = (throwable as? WooException)?.error?.message
        )
    }

    fun onShippingEdited(amount: BigDecimal, name: String) {
        tracker.track(
            ORDER_SHIPPING_METHOD_ADD,
            mapOf(KEY_FLOW to flow)
        )

        _orderDraft.update { draft ->
            val shipping: List<ShippingLine> = draft.shippingLines.mapIndexed { index, shippingLine ->
                if (index == 0) {
                    shippingLine.copy(total = amount, methodTitle = name)
                } else {
                    shippingLine
                }
            }.ifEmpty {
                listOf(ShippingLine(methodId = "other", total = amount, methodTitle = name))
            }

            draft.copy(shippingLines = shipping)
        }
    }

    fun onShippingRemoved() {
        tracker.track(
            ORDER_SHIPPING_METHOD_REMOVE,
            mapOf(KEY_FLOW to flow)
        )
        _orderDraft.update { draft ->
            draft.copy(
                shippingLines = draft.shippingLines.mapIndexed { index, shippingLine ->
                    if (index == 0) {
                        // Setting methodId to null will remove the shipping line in core
                        shippingLine.copy(methodId = null)
                    } else {
                        shippingLine
                    }
                }
            )
        }
    }

    fun onFeeEdited(feeValue: BigDecimal) {
        tracker.track(
            ORDER_FEE_ADD,
            mapOf(KEY_FLOW to flow)
        )

        _orderDraft.update { draft ->
            val fees: List<Order.FeeLine> = draft.feesLines.mapIndexed { index, feeLine ->
                if (index == 0) {
                    feeLine.copy(total = feeValue)
                } else {
                    feeLine
                }
            }.ifEmpty {
                listOf(
                    Order.FeeLine.EMPTY.copy(
                        name = ORDER_CUSTOM_FEE_NAME,
                        total = feeValue
                    )
                )
            }

            draft.copy(feesLines = fees)
        }
    }

    fun onFeeRemoved() {
        tracker.track(
            ORDER_FEE_REMOVE,
            mapOf(KEY_FLOW to flow)
        )
        _orderDraft.update { draft ->
            draft.copy(
                feesLines = draft.feesLines.mapIndexed { index, feeLine ->
                    if (index == 0) {
                        feeLine.copy(name = null)
                    } else {
                        feeLine
                    }
                }
            )
        }
    }

    @Parcelize
    data class ViewState(
        val isProgressDialogShown: Boolean = false,
        val willUpdateOrderDraft: Boolean = false,
        val isUpdatingOrderDraft: Boolean = false,
        val showOrderUpdateSnackbar: Boolean = false,
        val isEditable: Boolean = true,
        val multipleLinesContext: MultipleLinesContext = MultipleLinesContext.None
    ) : Parcelable {
        @IgnoredOnParcel
        val canCreateOrder: Boolean = !willUpdateOrderDraft && !isUpdatingOrderDraft && !showOrderUpdateSnackbar

        @IgnoredOnParcel
        val isIdle: Boolean = !isUpdatingOrderDraft && !willUpdateOrderDraft
    }

    sealed class Mode : Parcelable {
        @Parcelize
        object Creation : Mode()

        @Parcelize
        data class Edit(val orderId: Long) : Mode()
    }

    sealed class MultipleLinesContext : Parcelable {
        @Parcelize
        object None : MultipleLinesContext()

        @Parcelize
        data class Warning(
            val header: String,
            val explanation: String,
        ) : MultipleLinesContext()
    }
}

data class ProductUIModel(
    val item: Order.Item,
    val imageUrl: String,
    val isStockManaged: Boolean,
    val stockQuantity: Double,
    val stockStatus: ProductStockStatus
)

fun Order.Item.isSynced() = this.itemId != 0L
