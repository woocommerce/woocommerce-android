package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.*
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.mapAsync
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigationTarget.*
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val orderCreationRepository: OrderCreationRepository,
    private val mapItemToProductUiModel: MapItemToProductUiModel,
    private val createOrderItem: CreateOrderItem,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val PARAMETERS_KEY = "parameters_key"
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val orderDraftData = LiveDataDelegate(savedState, Order.EMPTY)
    private var orderDraft by orderDraftData

    val orderStatusData: LiveData<OrderStatus> = orderDraftData.liveData
        .map { it.status }
        .distinctUntilChanged()
        .mapAsync { status ->
            withContext(dispatchers.io) {
                orderDetailRepository.getOrderStatus(status.value)
            }
        }

    val products: LiveData<List<ProductUIModel>> = orderDraftData.liveData
        .map { it.items }
        .distinctUntilChanged()
        .mapAsync { items ->
            items.map { item -> mapItemToProductUiModel(item) }
        }

    val currentDraft
        get() = orderDraft

    init {
        orderDraft = orderDraft.copy(
            currency = parameterRepository.getParameters(PARAMETERS_KEY, savedState).currencyCode.orEmpty()
        )
    }

    fun onOrderStatusChanged(status: Order.Status) {
        orderDraft = orderDraft.copy(status = status)
    }

    fun onCustomerNoteEdited(newNote: String) {
        orderDraft = orderDraft.copy(customerNote = newNote)
    }

    fun onIncreaseProductsQuantity(id: Long) {
        orderDraft = orderDraft.adjustProductQuantity(id, +1)
    }

    fun onDecreaseProductsQuantity(id: Long) {
        orderDraft = orderDraft.adjustProductQuantity(id, -1)
    }

    fun onRemoveProduct(item: Order.Item) {
        orderDraft = orderDraft.updateItems(orderDraft.items - item)
    }

    fun onProductSelected(remoteProductId: Long, variationId: Long? = null) {
        val uniqueId = variationId ?: remoteProductId
        viewModelScope.launch {
            orderDraft.items.toMutableList().apply {
                val index = indexOfFirst { it.uniqueId == uniqueId }
                if (index != -1) {
                    val item = get(index)
                    set(index, item.copy(quantity = item.quantity + 1))
                    return@apply
                }
                // Create a new item
                val item = createOrderItem(remoteProductId, variationId)
                add(item)
            }.let { orderDraft = orderDraft.updateItems(it) }
        }
    }

    fun onCustomerAddressEdited(billingAddress: Address, shippingAddress: Address) {
        orderDraft = orderDraft.copy(
            billingAddress = billingAddress,
            shippingAddress = shippingAddress
        )
    }

    fun onEditOrderStatusClicked(currentStatus: OrderStatus) {
        launch(dispatchers.io) {
            orderDetailRepository
                .getOrderStatusOptions().toTypedArray()
                .runWithContext(dispatchers.main) {
                    triggerEvent(ViewOrderStatusSelector(currentStatus = currentStatus.statusKey, orderStatusList = it))
                }
        }
    }

    fun onCustomerClicked() {
        triggerEvent(EditCustomer)
    }

    fun onCustomerNoteClicked() {
        triggerEvent(EditCustomerNote)
    }

    fun onAddSimpleProductsClicked() {
        triggerEvent(AddProduct)
    }

    fun onProductClicked(item: Order.Item) {
        triggerEvent(ShowProductDetails(item))
    }

    fun onCreateOrderClicked(order: Order) {
        viewModelScope.launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            orderCreationRepository.createOrder(order).fold(
                onSuccess = {
                    triggerEvent(ShowSnackbar(string.order_creation_success_snackbar))
                    triggerEvent(ShowCreatedOrder(it.id))
                },
                onFailure = {
                    viewState = viewState.copy(isProgressDialogShown = false)
                    triggerEvent(ShowSnackbar(string.order_creation_failure_snackbar))
                }
            )
        }
    }

    @Parcelize
    data class ViewState(
        val isProgressDialogShown: Boolean = false
    ) : Parcelable
}

data class ProductUIModel(
    val item: Order.Item,
    val imageUrl: String,
    val isStockManaged: Boolean,
    val stockQuantity: Double,
    val canDecreaseQuantity: Boolean
)

val Order.isValidForCreation: Boolean
    get() = items.isNotEmpty() &&
        shippingAddress != Address.EMPTY &&
        billingAddress != Address.EMPTY
