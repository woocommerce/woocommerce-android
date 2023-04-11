package com.woocommerce.android.ui.login.storecreation.installation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionId
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.woocommerce.android.analytics.AnalyticsEvent.SITE_CREATION_WAITING_TIME
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
class InstallationTransactionLauncher @Inject constructor(
    private val performanceTransactionRepository: PerformanceTransactionRepository,
    dispatchers: CoroutineDispatchers,
) : LifecycleEventObserver {
    private companion object {
        const val TRANSACTION_NAME = "SiteInstallation"
    }

    private var performanceTransactionId: TransactionId? = null
    private val conditionsToSatisfy = MutableStateFlow(Conditions.values().toList())
    private val validatorScope = CoroutineScope(dispatchers.main + Job())
    private val waitingTimeTracker = WaitingTimeTracker(SITE_CREATION_WAITING_TIME)

    init {
        validatorScope.launch {
            conditionsToSatisfy.collect { toSatisfy ->
                if (toSatisfy.isEmpty()) {
                    performanceTransactionId?.let {
                        performanceTransactionRepository.finishTransaction(
                            it,
                            TransactionStatus.SUCCESSFUL
                        )
                    }
                    waitingTimeTracker.end()
                }
            }
        }
    }

    private enum class Conditions {
        STORE_INSTALLED,
    }

    fun onStoreInstallationRequested() {
        performanceTransactionId =
            performanceTransactionRepository.startTransaction(
                TRANSACTION_NAME,
                TransactionOperation.UI_LOAD
            )
        waitingTimeTracker.start()
    }

    fun onStoreInstalled() = satisfyCondition(Conditions.STORE_INSTALLED)

    fun onStoreInstallationFailed() {
        abortTracking()
    }

    private fun abortTracking() {
        performanceTransactionId?.let {
            performanceTransactionRepository.finishTransaction(
                it,
                TransactionStatus.ABORTED
            )
        }
        performanceTransactionId = null
        waitingTimeTracker.abort()
    }

    fun clear() {
        validatorScope.cancel()
    }

    private fun satisfyCondition(condition: Conditions) {
        conditionsToSatisfy.value = (conditionsToSatisfy.value - condition)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_DESTROY -> {
                abortTracking()
            }

            else -> {
                // no-op
            }
        }
    }
}
