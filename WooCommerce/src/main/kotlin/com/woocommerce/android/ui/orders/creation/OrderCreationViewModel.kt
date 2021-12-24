package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.woocommerce.android.extensions.mapAsync
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
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OrderCreationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val variationDetailRepository: VariationDetailRepository,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val PARAMETERS_KEY = "parameters_key"
    }

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
            items.map { item -> item.toProductUIModel() }
        }

    val currentDraft
        get() = orderDraft

    init {
        orderDraft = orderDraft.copy(
            currency = parameterRepository.getParameters(PARAMETERS_KEY, savedState).currencySymbol.orEmpty()
        )
    }

    fun onOrderStatusChanged(status: Order.Status) {
        orderDraft = orderDraft.copy(status = status)
    }

    fun onCustomerNoteEdited(newNote: String) {
        orderDraft = orderDraft.copy(customerNote = newNote)
    }

    private suspend fun Order.Item.toProductUIModel(): ProductUIModel {
        val (isStockManaged, stockQuantity) = withContext(dispatchers.io) {
            if (isVariation) {
                val variation = variationDetailRepository.getVariation(productId, variationId)
                Pair(variation?.isStockManaged, variation?.stockQuantity)
            } else {
                val product = productDetailRepository.getProduct(productId)
                Pair(product?.isStockManaged, product?.stockQuantity)
            }
        }
        return ProductUIModel(
            item = this,
            isStockManaged = isStockManaged ?: false,
            stockQuantity = stockQuantity ?: 0.0
        )
    }
}

data class ProductUIModel(
    val item: Order.Item,
    val isStockManaged: Boolean,
    val stockQuantity: Double
)
