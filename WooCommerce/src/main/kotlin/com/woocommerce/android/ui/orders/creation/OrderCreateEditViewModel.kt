package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@ExperimentalCoroutinesApi
abstract class OrderCreateEditViewModel(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val mapItemToProductUiModel: MapItemToProductUiModel,
    private val createOrderItem: CreateOrderItem
) : ScopedViewModel(savedState) {
    companion object {
        const val PARAMETERS_KEY = "parameters_key"
        const val ORDER_CUSTOM_FEE_NAME = "order_custom_fee"
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    protected var viewState by viewStateData

    protected val _orderDraft: MutableStateFlow<Order> = savedState.getStateFlow(viewModelScope, Order.EMPTY)
    val orderDraft = _orderDraft.asLiveData()

    val orderStatusData: LiveData<Order.OrderStatus> = _orderDraft
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

    protected val retryOrderDraftUpdateTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    protected abstract val syncStrategy: SyncStrategy

    fun getProductUIModelFromItem(item: Order.Item) = runBlocking {
        mapItemToProductUiModel(item)
    }

    val currentDraft
        get() = _orderDraft.value

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
                AnalyticsTracker.KEY_FROM to _orderDraft.value.status.value,
                AnalyticsTracker.KEY_TO to status.value,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_FLOW_CREATION
            )
        )
        _orderDraft.update { it.copy(status = status) }
    }

    fun onRemoveProduct(item: Order.Item) {
        _orderDraft.update {
            it.adjustProductQuantity(item.itemId, -item.quantity.toInt())
        }
    }

    fun onProductSelected(remoteProductId: Long, variationId: Long? = null) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_PRODUCT_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_FLOW_CREATION)
        )

        viewModelScope.launch {
            _orderDraft.value.items.toMutableList().apply {
                add(createOrderItem(remoteProductId, variationId))
            }.let { items ->
                _orderDraft.update { it.updateItems(items) }
            }
        }
    }

    fun onCustomerAddressEdited(billingAddress: Address, shippingAddress: Address) {
        val hasDifferentShippingDetails = _orderDraft.value.shippingAddress != _orderDraft.value.billingAddress
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_CUSTOMER_ADD,
            mapOf(
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_FLOW_CREATION,
                AnalyticsTracker.KEY_HAS_DIFFERENT_SHIPPING_DETAILS to hasDifferentShippingDetails
            )
        )

        _orderDraft.update {
            it.copy(
                billingAddress = billingAddress,
                shippingAddress = shippingAddress.takeIf { it != Address.EMPTY } ?: billingAddress
            )
        }
    }

    fun onEditOrderStatusClicked(currentStatus: Order.OrderStatus) {
        launch(dispatchers.io) {
            orderDetailRepository
                .getOrderStatusOptions().toTypedArray()
                .runWithContext(dispatchers.main) {
                    triggerEvent(
                        OrderNavigationTarget.ViewOrderStatusSelector(
                            currentStatus = currentStatus.statusKey,
                            orderStatusList = it
                        )
                    )
                }
        }
    }

    fun onCustomerClicked() {
        triggerEvent(OrderCreationNavigationTarget.EditCustomer)
    }

    fun onCustomerNoteClicked() {
        triggerEvent(OrderCreationNavigationTarget.EditCustomerNote)
    }

    fun onAddProductClicked() {
        triggerEvent(OrderCreationNavigationTarget.AddProduct)
    }

    fun onProductClicked(item: Order.Item) {
        // Don't show details if the product is not synced yet
        if (!item.isSynced()) return
        triggerEvent(OrderCreationNavigationTarget.ShowProductDetails(item))
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
        triggerEvent(OrderCreationNavigationTarget.EditFee(orderSubtotal, currentFeeValue))
    }

    fun onShippingButtonClicked() {
        triggerEvent(OrderCreationNavigationTarget.EditShipping(currentDraft.shippingLines.firstOrNull { it.methodId != null }))
    }

    fun onShippingEdited(amount: BigDecimal, name: String) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_SHIPPING_METHOD_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_FLOW_CREATION)
        )

        _orderDraft.update { draft ->
            val shipping: List<Order.ShippingLine> = draft.shippingLines.mapIndexed { index, shippingLine ->
                if (index == 0) {
                    shippingLine.copy(total = amount, methodTitle = name)
                } else {
                    shippingLine
                }
            }.ifEmpty {
                listOf(Order.ShippingLine(methodId = "other", total = amount, methodTitle = name))
            }

            draft.copy(shippingLines = shipping)
        }
    }

    fun onShippingRemoved() {
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
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_FEE_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_FLOW_CREATION)
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

    abstract fun onSaveOrderClicked()
    abstract fun onBackButtonClicked()
    protected abstract fun monitorOrderChanges()
    protected abstract fun trackOrderSaveFailure(it: Throwable)
    protected abstract fun trackSaveOrderButtonClick()

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
