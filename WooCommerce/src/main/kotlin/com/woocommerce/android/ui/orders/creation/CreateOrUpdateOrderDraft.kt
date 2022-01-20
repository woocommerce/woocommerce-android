package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.extensions.areSameAs
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

class CreateOrUpdateOrderDraft(
    private val dispatchers: CoroutineDispatchers,
    private val orderCreationRepository: OrderCreationRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    operator fun invoke(changes: Flow<Order>): Flow<OrderDraftUpdateStatus> {
        return changes
            .filter { it.items.isNotEmpty() }
            .distinctUntilChanged { old, new ->
                areEquivalent(old, new)
            }
            .debounce(1000)
            .flowOn(dispatchers.computation)
            .transformLatest {
                emit(OrderDraftUpdateStatus.Ongoing)
                orderCreationRepository.createOrUpdateDraft(it)
                    .fold(
                        onSuccess = { emit(OrderDraftUpdateStatus.Succeeded(it)) },
                        onFailure = { emit(OrderDraftUpdateStatus.Failed) }
                    )
            }
    }

    sealed interface OrderDraftUpdateStatus {
        object Ongoing : OrderDraftUpdateStatus
        data class Succeeded(val order: Order) : OrderDraftUpdateStatus
        object Failed : OrderDraftUpdateStatus
    }

    private fun areEquivalent(old: Order, new: Order): Boolean {
        // Make sure to update the prices only when items did change
        // TODO we need to include more checks here: fees and discounts...
        return old.items.areSameAs(new.items) { newItem ->
            this.productId == newItem.productId &&
                this.variationId == newItem.variationId &&
                this.quantity == newItem.quantity
        }
    }
}
