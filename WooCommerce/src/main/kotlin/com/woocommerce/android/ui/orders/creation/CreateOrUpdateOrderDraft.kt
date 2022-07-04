package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.extensions.areSameAs
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class CreateOrUpdateOrderDraft @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val orderCreationRepository: OrderCreationRepository
) {
    companion object {
        const val DEBOUNCE_DURATION_MS = 500L
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(changes: Flow<Order>, retryTrigger: Flow<Unit>): Flow<OrderDraftUpdateStatus> {
        return changes
            .distinctUntilChanged(::areEquivalent)
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
                    .flatMapLatest { createOrUpdateDraft(it) }
                    .onStart { emit(OrderDraftUpdateStatus.PendingDebounce) }
                    .distinctUntilChanged()
            }
    }

    private fun createOrUpdateDraft(order: Order) = flow {
        emit(OrderDraftUpdateStatus.Ongoing)
        orderCreationRepository.createOrUpdateDraft(order)
            .fold(
                onSuccess = { emit(OrderDraftUpdateStatus.Succeeded(it)) },
                onFailure = { emit(OrderDraftUpdateStatus.Failed) }
            )
    }

    sealed interface OrderDraftUpdateStatus {
        object PendingDebounce : OrderDraftUpdateStatus
        object Ongoing : OrderDraftUpdateStatus
        data class Succeeded(val order: Order) : OrderDraftUpdateStatus
        object Failed : OrderDraftUpdateStatus
    }

    private fun areEquivalent(old: Order, new: Order): Boolean {
        // Make sure to update the prices only when items did change
        val hasSameItems = old.items
            .filter {
                // Check only non-zero quantities, to avoid circular update when removing products
                it.quantity > 0
            }
            .areSameAs(new.items) { newItem ->
                // TODO M3: we need probably to compare the totals too, to account for discounts
                this.productId == newItem.productId &&
                    this.variationId == newItem.variationId &&
                    this.quantity == newItem.quantity
            }

        val hasSameShippingLines = old.shippingLines
            .filter {
                // Check only non-removed shipping lines to avoid circular update when removing
                it.methodId != null
            }
            .areSameAs(new.shippingLines) { newLine ->
                this.methodId == newLine.methodId &&
                    this.methodTitle == newLine.methodTitle &&
                    this.total isEqualTo newLine.total
            }

        val hasSameFeeLines = old.feesLines
            .filter { it.name != null }
            .areSameAs(new.feesLines) { newLine ->
                this.name == newLine.name &&
                    this.total isEqualTo newLine.total
            }

        val hasSameStatus = old.status == new.status

        return hasSameItems &&
            hasSameShippingLines &&
            hasSameFeeLines &&
            old.shippingAddress.isSamePhysicalAddress(new.shippingAddress) &&
            old.billingAddress.isSamePhysicalAddress(new.billingAddress) &&
            hasSameStatus
    }
}
