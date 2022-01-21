package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.extensions.areSameAs
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val DEBOUNCE_DURATION_MS = 1000L

class CreateOrUpdateOrderDraft @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val orderCreationRepository: OrderCreationRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    operator fun invoke(changes: Flow<Order>, retryTrigger: Flow<Unit>): Flow<OrderDraftUpdateStatus> {
        return changes
            .filter { it.items.isNotEmpty() }
            .distinctUntilChanged { old, new ->
                areEquivalent(old, new)
            }
            .flowOn(dispatchers.computation)
            .flatMapLatest {
                val debouncedChanges = flow {
                    // We can't use `debounce` directly, because we want to cancel any current update
                    // when we get new changes, hence the use of `flatMapLatest` + `delay`
                    delay(DEBOUNCE_DURATION_MS)
                    emit(it)
                }
                debouncedChanges
                    .combine(retryTrigger.onStart { emit(Unit) }) { draft, _ -> draft }
                    .transform { order ->
                        emit(OrderDraftUpdateStatus.Ongoing)
                        orderCreationRepository.createOrUpdateDraft(order)
                            .fold(
                                onSuccess = { emit(OrderDraftUpdateStatus.Succeeded(it)) },
                                onFailure = { emit(OrderDraftUpdateStatus.Failed) }
                            )
                    }
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
        return old.items
            .filter {
                // Check only non-zero quantities, to avoid circular update when removing products
                it.quantity > 0
            }
            .areSameAs(new.items) { newItem ->
                this.productId == newItem.productId &&
                    this.variationId == newItem.variationId &&
                    this.quantity == newItem.quantity
            }
    }
}
