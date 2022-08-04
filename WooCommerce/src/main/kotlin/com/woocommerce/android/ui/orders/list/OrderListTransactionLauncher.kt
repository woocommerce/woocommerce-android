package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionId
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.woocommerce.android.ui.orders.list.OrderListTransactionLauncher.Conditions.FIRST_LIST_FETCHED
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelScoped
class OrderListTransactionLauncher @Inject constructor(
    private val performanceTransactionRepository: PerformanceTransactionRepository,
    private val dispatchers: CoroutineDispatchers,
) : LifecycleEventObserver {
    private companion object {
        const val TRANSACTION_NAME = "OrderList"
    }

    private var performanceTransactionId: TransactionId? = null
    private val conditionsToSatisfy = MutableStateFlow(Conditions.values().toList())
    private val validatorScope = CoroutineScope(dispatchers.main + Job())

    init {
        validatorScope.launch {
            conditionsToSatisfy.collect { toSatisfy ->
                if (toSatisfy.isEmpty()) {
                    performanceTransactionId?.let {
                        performanceTransactionRepository.finishTransaction(it, TransactionStatus.SUCCESSFUL)
                    }
                }
            }
        }
    }

    private enum class Conditions {
        FIRST_LIST_FETCHED,
    }

    fun onListFetched() = satisfyCondition(FIRST_LIST_FETCHED)

    fun clear() {
        validatorScope.cancel()
    }

    private fun satisfyCondition(condition: Conditions) {
        conditionsToSatisfy.value = (conditionsToSatisfy.value - condition)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                performanceTransactionId =
                    performanceTransactionRepository.startTransaction(TRANSACTION_NAME, TransactionOperation.UI_LOAD)
            }
            Lifecycle.Event.ON_STOP -> {
                performanceTransactionId?.let {
                    performanceTransactionRepository.finishTransaction(it, TransactionStatus.ABORTED)
                }
            }
            else -> {
                // no-op
            }
        }
    }
}
