package com.woocommerce.android.ui.orders.details

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionId
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_DETAIL_WAITING_TIME_LOADED
import com.woocommerce.android.analytics.WaitingTimeTracker
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelScoped
class OrderDetailsTransactionLauncher @Inject constructor(
    private val performanceTransactionRepository: PerformanceTransactionRepository,
    dispatchers: CoroutineDispatchers,
) : LifecycleEventObserver {
    private companion object {
        const val TRANSACTION_NAME = "OrderDetails"
    }

    private var performanceTransactionId: TransactionId? = null
    private val conditionsToSatisfy = MutableStateFlow(Conditions.values().toList())
    private val validatorScope = CoroutineScope(dispatchers.main + Job())
    private val waitingTimeTracker = WaitingTimeTracker(ORDER_DETAIL_WAITING_TIME_LOADED)

    init {
        validatorScope.launch {
            conditionsToSatisfy.collect { toSatisfy ->
                if (toSatisfy.isEmpty()) {
                    performanceTransactionId?.let {
                        performanceTransactionRepository.finishTransaction(it, TransactionStatus.SUCCESSFUL)
                    }
                    waitingTimeTracker.end()
                }
            }
        }
    }

    private enum class Conditions {
        ORDER_FETCHED,
        SHIPPING_LABEL_FETCHED,
        NOTES_FETCHED,
        REFUNDS_FETCHED,
        SHIPMENT_TRACKINGS_FETCHED,
        PACKAGE_CREATION_ELIGIBLE_FETCHED
    }

    fun onOrderFetched() = satisfyCondition(Conditions.ORDER_FETCHED)

    fun onShippingLabelFetchingCompleted() = satisfyCondition(Conditions.SHIPPING_LABEL_FETCHED)

    fun onNotesFetched() = satisfyCondition(Conditions.NOTES_FETCHED)

    fun onRefundsFetched() = satisfyCondition(Conditions.REFUNDS_FETCHED)

    fun onShipmentTrackingFetchingCompleted() = satisfyCondition(Conditions.SHIPMENT_TRACKINGS_FETCHED)

    fun onPackageCreationEligibleFetched() = satisfyCondition(Conditions.PACKAGE_CREATION_ELIGIBLE_FETCHED)

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
                waitingTimeTracker.start()
            }
            Lifecycle.Event.ON_DESTROY -> {
                performanceTransactionId?.let {
                    performanceTransactionRepository.finishTransaction(it, TransactionStatus.ABORTED)
                }
                performanceTransactionId = null
                waitingTimeTracker.abort()
            }
            else -> {
                // no-op
            }
        }
    }
}
