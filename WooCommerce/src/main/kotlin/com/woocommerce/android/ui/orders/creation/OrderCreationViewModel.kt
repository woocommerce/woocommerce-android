package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FROM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_CUSTOMER_DETAILS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_DIFFERENT_SHIPPING_DETAILS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATUS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_TO
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_CREATION
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.*
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
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
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val _orderDraft = savedState.getStateFlow(viewModelScope, Order.EMPTY)
    val orderDraft = _orderDraft
        .onEach {
            viewState = viewState.copy(
                isOrderValidForCreation = it.items.isNotEmpty() &&
                    it.shippingAddress != Address.EMPTY &&
                    it.billingAddress != Address.EMPTY
            )
        }
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

    val currentDraft
        get() = _orderDraft.value

    init {
        _orderDraft.update {
            it.copy(currency = parameterRepository.getParameters(PARAMETERS_KEY, savedState).currencyCode.orEmpty())
        }
        monitorOrderChanges()
    }

    fun onCustomerNoteEdited(newNote: String) = _orderDraft.update { it.copy(customerNote = newNote) }

    fun onIncreaseProductsQuantity(id: Long) = _orderDraft.update { it.adjustProductQuantity(id, +1) }

    fun onDecreaseProductsQuantity(id: Long) = _orderDraft.update { it.adjustProductQuantity(id, -1) }

    fun onOrderStatusChanged(status: Order.Status) {
        AnalyticsTracker.track(
            Stat.ORDER_STATUS_CHANGE,
            mapOf(
                KEY_FROM to _orderDraft.value.status.value,
                KEY_TO to status.value,
                KEY_FLOW to VALUE_FLOW_CREATION
            )
        )
        _orderDraft.update { it.copy(status = status) }
    }

    fun onRemoveProduct(item: Order.Item) = _orderDraft.update {
        if (FeatureFlag.ORDER_CREATION_M2.isEnabled()) {
            it.adjustProductQuantity(item.uniqueId, -item.quantity.toInt())
        } else {
            it.updateItems(it.items - item)
        }
    }

    fun onProductSelected(remoteProductId: Long, variationId: Long? = null) {
        AnalyticsTracker.track(
            Stat.ORDER_PRODUCT_ADD,
            mapOf(KEY_FLOW to VALUE_FLOW_CREATION)
        )
        val uniqueId = variationId ?: remoteProductId
        viewModelScope.launch {
            _orderDraft.value.items.toMutableList().apply {
                val index = indexOfFirst { it.uniqueId == uniqueId }
                if (index != -1) {
                    val item = get(index)
                    set(index, item.copy(quantity = item.quantity + 1))
                    return@apply
                }
                // Create a new item
                val item = createOrderItem(remoteProductId, variationId)
                add(item)
            }.let { items -> _orderDraft.update { it.updateItems(items) } }
        }
    }

    fun onCustomerAddressEdited(billingAddress: Address, shippingAddress: Address) {
        val hasDifferentShippingDetails = _orderDraft.value.shippingAddress != _orderDraft.value.billingAddress
        AnalyticsTracker.track(
            Stat.ORDER_CUSTOMER_ADD,
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
        triggerEvent(ShowProductDetails(item))
    }

    fun onRetryPaymentsClicked() {
        retryOrderDraftUpdateTrigger.tryEmit(Unit)
    }

    fun onCreateOrderClicked(order: Order) {
        trackCreateOrderButtonClick()
        viewModelScope.launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            orderCreationRepository.placeOrder(order).fold(
                onSuccess = {
                    AnalyticsTracker.track(Stat.ORDER_CREATION_SUCCESS)
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
    }

    /**
     * Monitor order changes, and update the remote draft to update price totals
     */
    private fun monitorOrderChanges() {
        if (!FeatureFlag.ORDER_CREATION_M2.isEnabled()) return
        viewModelScope.launch {
            createOrUpdateOrderDraft(_orderDraft, retryOrderDraftUpdateTrigger)
                .collect { updateStatus ->
                    when (updateStatus) {
                        OrderDraftUpdateStatus.Ongoing ->
                            viewState = viewState.copy(isUpdatingOrderDraft = true, showOrderUpdateSnackbar = false)
                        OrderDraftUpdateStatus.Failed ->
                            viewState = viewState.copy(isUpdatingOrderDraft = false, showOrderUpdateSnackbar = true)
                        is OrderDraftUpdateStatus.Succeeded -> {
                            viewState = viewState.copy(isUpdatingOrderDraft = false, showOrderUpdateSnackbar = false)
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
            Stat.ORDER_CREATION_FAILED,
            mapOf(
                KEY_ERROR_CONTEXT to it::class.java.simpleName,
                KEY_ERROR_TYPE to it,
                KEY_ERROR_DESC to it.message
            )
        )
    }

    private fun trackCreateOrderButtonClick() {
        AnalyticsTracker.track(
            Stat.ORDER_CREATE_BUTTON_TAPPED,
            mapOf(
                KEY_STATUS to _orderDraft.value.status,
                KEY_PRODUCT_COUNT to products.value?.count(),
                KEY_HAS_CUSTOMER_DETAILS to _orderDraft.value.billingAddress.hasInfo()
            )
        )
    }

    @Parcelize
    data class ViewState(
        val isProgressDialogShown: Boolean = false,
        private val isOrderValidForCreation: Boolean = false,
        val isUpdatingOrderDraft: Boolean = false,
        val showOrderUpdateSnackbar: Boolean = false
    ) : Parcelable {
        @IgnoredOnParcel
        val canCreateOrder: Boolean = isOrderValidForCreation && !isUpdatingOrderDraft && !showOrderUpdateSnackbar
    }
}

data class ProductUIModel(
    val item: Order.Item,
    val imageUrl: String,
    val isStockManaged: Boolean,
    val stockQuantity: Double,
    val canDecreaseQuantity: Boolean
)
