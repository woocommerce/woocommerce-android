package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.mapAsync
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderCreationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val variationDetailRepository: VariationDetailRepository,
    private val parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val PARAMETERS_KEY = "parameters_key"
    }

    val orderDraftData = LiveDataDelegate(savedState, Order.EMPTY, onChange = ::onOrderDraftChange)
    private var orderDraft by orderDraftData

    private val orderStatus = MutableLiveData<OrderStatus>()
    val orderStatusData: LiveData<OrderStatus> = orderStatus

    val products: LiveData<List<ProductUIModel>> = orderDraftData.liveData.mapAsync { order ->
        order.items.map { item ->
            val stockQuantity = if (item.isVariation) {
                val variation = variationDetailRepository.getVariation(item.productId, item.variationId)
                if (variation?.isStockManaged == false) Double.MAX_VALUE else variation?.stockQuantity ?: 0.0
            } else {
                val product = productDetailRepository.getProduct(item.productId)
                if (product?.isStockManaged == false) Double.MAX_VALUE else product?.stockQuantity ?: 0.0
            }
            ProductUIModel(
                item = item,
                stockQuantity = stockQuantity
            )
        }
    }

    val currentDraft
        get() = orderDraft

    init {
        updateOrderStatus(orderDraft.status)
        orderDraft = orderDraft.copy(
            currency = parameterRepository.getParameters(PARAMETERS_KEY, savedState).currencySymbol.orEmpty()
        )

        orderDraft = orderDraft.copy(
            items = listOf(
                Order.Item(
                    0L,
                    productId = 44,
                    "Test Product",
                    price = BigDecimal.TEN,
                    sku = "SKU123",
                    quantity = 2f,
                    subtotal = BigDecimal.ONE,
                    totalTax = BigDecimal.TEN,
                    total = BigDecimal.TEN,
                    variationId = 0L,
                    attributesList = emptyList()
                ),
                Order.Item(
                    0L,
                    productId = 44,
                    "Test Product",
                    price = BigDecimal.TEN,
                    sku = "SKU123",
                    quantity = 2f,
                    subtotal = BigDecimal.ONE,
                    totalTax = BigDecimal.TEN,
                    total = BigDecimal.TEN,
                    variationId = 0L,
                    attributesList = emptyList()
                ),
                Order.Item(
                    0L,
                    productId = 44,
                    "Test Product",
                    price = BigDecimal.TEN,
                    sku = "SKU123",
                    quantity = 2f,
                    subtotal = BigDecimal.ONE,
                    totalTax = BigDecimal.TEN,
                    total = BigDecimal.TEN,
                    variationId = 0L,
                    attributesList = emptyList()
                )
            )
        )
    }

    fun onOrderStatusChanged(status: Order.Status) {
        orderDraft = orderDraft.copy(status = status)
    }

    private fun onOrderDraftChange(old: Order?, new: Order) {
        if (old?.status != new.status) {
            updateOrderStatus(new.status)
        }
    }

    private fun updateOrderStatus(status: Order.Status) {
        launch(dispatchers.io) {
            orderDetailRepository.getOrderStatus(status.value)
                .runWithContext(dispatchers.main) {
                    orderStatus.value = it
                }
        }
    }

    fun onCustomerNoteEdited(newNote: String) {
        orderDraft = orderDraft.copy(customerNote = newNote)
    }
}

data class ProductUIModel(
    val item: Order.Item,
    val stockQuantity: Double
)
