package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class CreateUpdateOrder @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val orderCreateEditRepository: OrderCreateEditRepository
) {
    companion object {
        const val DEBOUNCE_DURATION_MS = 500L
    }

    private fun createOrUpdateOrder(order: Order) = flow {
        emit(OrderUpdateStatus.Ongoing)
        orderCreateEditRepository.createOrUpdateOrder(order)
            .fold(
                onSuccess = { emit(OrderUpdateStatus.Succeeded(it)) },
                onFailure = { emit(OrderUpdateStatus.Failed(it)) }
            )
    }

    sealed interface OrderUpdateStatus {
        object PendingDebounce : OrderUpdateStatus
        object Ongoing : OrderUpdateStatus
        data class Succeeded(val order: Order) : OrderUpdateStatus
        data class Failed(val throwable: Throwable) : OrderUpdateStatus
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(changes: Flow<Order>, retryTrigger: Flow<Unit>): Flow<OrderUpdateStatus> {
        return changes
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
                    .flatMapLatest { createOrUpdateOrder(it) }
                    .onStart { emit(OrderUpdateStatus.PendingDebounce) }
                    .distinctUntilChanged()
            }
    }
}
