package com.woocommerce.android.ui.analytics

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionId
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.woocommerce.android.analytics.AnalyticsEvent.ANALYTICS_HUB_WAITING_TIME_LOADED
import com.woocommerce.android.analytics.WaitingTimeTracker
import com.woocommerce.android.ui.analytics.AnalyticsHubTransactionLauncher.Conditions.ORDERS_FETCHED
import com.woocommerce.android.ui.analytics.AnalyticsHubTransactionLauncher.Conditions.PRODUCTS_FETCHED
import com.woocommerce.android.ui.analytics.AnalyticsHubTransactionLauncher.Conditions.REVENUE_FETCHED
import com.woocommerce.android.ui.analytics.AnalyticsHubTransactionLauncher.Conditions.VISITORS_FETCHED
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelScoped
class AnalyticsHubTransactionLauncher @Inject constructor(
    private val performanceTransactionRepository: PerformanceTransactionRepository,
    dispatchers: CoroutineDispatchers,
) : LifecycleEventObserver {
    private companion object {
        const val TRANSACTION_NAME = "AnalyticsHub"
    }

    private var performanceTransactionId: TransactionId? = null
    private val conditionsToSatisfy = MutableStateFlow(Conditions.values().toList())
    private val validatorScope = CoroutineScope(dispatchers.main + Job())
    private val waitingTimeTracker = WaitingTimeTracker(ANALYTICS_HUB_WAITING_TIME_LOADED)

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
        REVENUE_FETCHED,
        ORDERS_FETCHED,
        PRODUCTS_FETCHED,
        VISITORS_FETCHED
    }

    fun onRevenueFetched() = satisfyCondition(REVENUE_FETCHED)

    fun onOrdersFetched() = satisfyCondition(ORDERS_FETCHED)

    fun onProductsFetched() = satisfyCondition(PRODUCTS_FETCHED)

    fun onVisitorsFetched() = satisfyCondition(VISITORS_FETCHED)

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
            Lifecycle.Event.ON_STOP -> {
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
