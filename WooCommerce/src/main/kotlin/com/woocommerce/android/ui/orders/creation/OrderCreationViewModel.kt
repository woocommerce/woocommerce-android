package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R.string
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
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
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATUS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_TO
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_CREATION
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.Order.ShippingLine
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.AddProduct
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.EditCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.EditFee
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.EditShipping
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.ShowProductDetails
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
class OrderCreationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val orderCreationRepository: OrderCreationRepository,
    private val mapItemToProductUiModel: MapItemToProductUiModel,
    private val createOrUpdateOrderDraft: CreateOrUpdateOrderDraft,
    private val createOrderItem: CreateOrderItem,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val PARAMETERS_KEY = "parameters_key"
        private const val ORDER_CUSTOM_FEE_NAME = "order_custom_fee"
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val args: OrderCreationFormFragmentArgs by savedState.navArgs()
    private val mode: Mode = args.mode
    private var initialOrder: Order? = null

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

    fun getProductUIModelFromItem(item: Order.Item) = runBlocking {
        mapItemToProductUiModel(item)
    }

    val currentDraft
        get() = _orderDraft.value

    init {
        _orderDraft.update {
            it.copy(currency = parameterRepository.getParameters(PARAMETERS_KEY, savedState).currencyCode.orEmpty())
        }
        monitorOrderChanges()

        if (mode is Mode.Edit)
            viewModelScope.launch {
                orderDetailRepository.getOrderById(mode.orderId).let {
                    if (it != null) {
                        _orderDraft.value = it
                        initialOrder = it
                    }
                }
            }
    }

    fun onCustomerNoteEdited(newNote: String) = _orderDraft.update { it.copy(customerNote = newNote) }

    fun onIncreaseProductsQuantity(id: Long) = _orderDraft.update { it.adjustProductQuantity(id, +1) }

    fun onDecreaseProductsQuantity(id: Long) {
        _orderDraft.value.items
            .find { it.itemId == id }
            ?.takeIf { it.quantity == 1F }
            ?.let { onProductClicked(it) }
            ?: _orderDraft.update { it.adjustProductQuantity(id, -1) }
    }

    fun onOrderStatusChanged(status: Order.Status) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_STATUS_CHANGE,
            mapOf(
                KEY_FROM to _orderDraft.value.status.value,
                KEY_TO to status.value,
                KEY_FLOW to VALUE_FLOW_CREATION
            )
        )
        _orderDraft.update { it.copy(status = status) }
    }

    fun onRemoveProduct(item: Order.Item) = _orderDraft.update {
        it.adjustProductQuantity(item.itemId, -item.quantity.toInt())
    }

    fun onProductSelected(remoteProductId: Long, variationId: Long? = null) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_PRODUCT_ADD,
            mapOf(KEY_FLOW to VALUE_FLOW_CREATION)
        )

        viewModelScope.launch {
            _orderDraft.value.items.toMutableList().apply {
                add(createOrderItem(remoteProductId, variationId))
            }.let { items -> _orderDraft.update { it.updateItems(items) } }
        }
    }

    fun onCustomerAddressEdited(billingAddress: Address, shippingAddress: Address) {
        val hasDifferentShippingDetails = _orderDraft.value.shippingAddress != _orderDraft.value.billingAddress
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_CUSTOMER_ADD,
            mapOf(
                KEY_FLOW to VALUE_FLOW_CREATION,
                KEY_HAS_DIFFERENT_SHIPPING_DETAILS to hasDifferentShippingDetails
            )
        )

        _orderDraft.update {
            it.copy(
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
        trackCreateOrderButtonClick()
        when (mode) {
            Mode.Creation -> viewModelScope.launch {
                viewState = viewState.copy(isProgressDialogShown = true)
                orderCreationRepository.placeOrder(order).fold(
                    onSuccess = {
                        AnalyticsTracker.track(AnalyticsEvent.ORDER_CREATION_SUCCESS)
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
                                    launch { orderCreationRepository.deleteDraftOrder(draft) }
                                }
                                triggerEvent(Exit)
                            }
                        )
                    )
                }
            }
            is Mode.Edit -> {
                if (_orderDraft.value == initialOrder) {
                    triggerEvent(Exit)
                } else {
                    triggerEvent(
                        ShowDialog.buildDiscardDialogEvent(
                            positiveBtnAction = { _, _ ->
                                launch {
                                    initialOrder?.let { orderCreationRepository.placeOrder(it) }
                                    triggerEvent(Exit)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    /**
     * Monitor order changes, and update the remote draft to update price totals
     */
    private fun monitorOrderChanges() {
        viewModelScope.launch {
            createOrUpdateOrderDraft(_orderDraft.drop(1), retryOrderDraftUpdateTrigger)
                .collect { updateStatus ->
                    when (updateStatus) {
                        OrderDraftUpdateStatus.PendingDebounce ->
                            viewState = viewState.copy(willUpdateOrderDraft = true, showOrderUpdateSnackbar = false)
                        OrderDraftUpdateStatus.Ongoing ->
                            viewState = viewState.copy(willUpdateOrderDraft = false, isUpdatingOrderDraft = true)
                        OrderDraftUpdateStatus.Failed ->
                            viewState = viewState.copy(isUpdatingOrderDraft = false, showOrderUpdateSnackbar = true)
                        is OrderDraftUpdateStatus.Succeeded -> {
                            viewState = viewState.copy(
                                isUpdatingOrderDraft = false,
                                showOrderUpdateSnackbar = false,
                                isEditable = updateStatus.order.isEditable || mode is Mode.Creation
                            )
                            _orderDraft.update { currentDraft ->
                                // Keep the user's selected status
                                updateStatus.order.copy(status = currentDraft.status)
                            }
                        }
                    }
                }
        }
    }

    private fun trackOrderCreationFailure(it: Throwable) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_CREATION_FAILED,
            mapOf(
                KEY_ERROR_CONTEXT to this::class.java.simpleName,
                KEY_ERROR_TYPE to (it as? WooException)?.error?.type?.name,
                KEY_ERROR_DESC to it.message
            )
        )
    }

    private fun trackCreateOrderButtonClick() {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_CREATE_BUTTON_TAPPED,
            mapOf(
                KEY_STATUS to _orderDraft.value.status,
                KEY_PRODUCT_COUNT to products.value?.count(),
                KEY_HAS_CUSTOMER_DETAILS to _orderDraft.value.billingAddress.hasInfo(),
                KEY_HAS_FEES to _orderDraft.value.feesLines.isNotEmpty(),
                KEY_HAS_SHIPPING_METHOD to _orderDraft.value.shippingLines.isNotEmpty()
            )
        )
    }

    fun onShippingEdited(amount: BigDecimal, name: String) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_SHIPPING_METHOD_ADD,
            mapOf(KEY_FLOW to VALUE_FLOW_CREATION)
        )

        _orderDraft.update { draft ->
            val shipping = draft.shippingLines.firstOrNull()?.copy(total = amount, methodTitle = name)
                ?: ShippingLine(methodId = "other", total = amount, methodTitle = name)
            draft.copy(shippingLines = listOf(shipping))
        }
    }

    fun onShippingRemoved() {
        _orderDraft.update { draft ->
            // We are iterating over all shipping lines, but on the current feature, we support only one shipping item
            val shippingLines = draft.shippingLines.map {
                it.copy(methodId = null)
            }
            draft.copy(shippingLines = shippingLines)
        }
    }

    fun onFeeEdited(feeValue: BigDecimal) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_FEE_ADD,
            mapOf(KEY_FLOW to VALUE_FLOW_CREATION)
        )

        val newFee = _orderDraft.value.feesLines.firstOrNull { it.name != null }
            ?: Order.FeeLine.EMPTY

        _orderDraft.update { draft ->
            listOf(newFee.copy(name = ORDER_CUSTOM_FEE_NAME, total = feeValue))
                .let { draft.copy(feesLines = it) }
        }
    }

    fun onFeeRemoved() {
        _orderDraft.update { draft ->
            draft.feesLines
                .map { it.copy(name = null) }
                .let { draft.copy(feesLines = it) }
        }
    }

    @Parcelize
    data class ViewState(
        val isProgressDialogShown: Boolean = false,
        val willUpdateOrderDraft: Boolean = false,
        val isUpdatingOrderDraft: Boolean = false,
        val showOrderUpdateSnackbar: Boolean = false,
        val isEditable: Boolean = true
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
}

data class ProductUIModel(
    val item: Order.Item,
    val imageUrl: String,
    val isStockManaged: Boolean,
    val stockQuantity: Double,
    val stockStatus: ProductStockStatus
)

fun Order.Item.isSynced() = this.itemId != 0L
