package com.woocommerce.android.ui.login.storecreation.installation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionId
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_WOOCOMMERCE_SITE_CREATED
import com.woocommerce.android.analytics.WaitingTimeTracker
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class InstallationTransactionLauncher @Inject constructor(
    private val performanceTransactionRepository: PerformanceTransactionRepository,
) : LifecycleEventObserver {
    private companion object {
        const val TRANSACTION_NAME = "SiteInstallation"
    }

    private var performanceTransactionId: TransactionId? = null
    private val waitingTimeTracker = WaitingTimeTracker(LOGIN_WOOCOMMERCE_SITE_CREATED)

    fun onStoreInstallationRequested() {
        performanceTransactionId =
            performanceTransactionRepository.startTransaction(
                TRANSACTION_NAME,
                TransactionOperation.UI_LOAD
            )
        waitingTimeTracker.start()
    }

    fun onStoreInstalled(parameters: Map<String, *>) {
        performanceTransactionId?.let {
            performanceTransactionRepository.finishTransaction(
                it,
                TransactionStatus.SUCCESSFUL
            )
        }
        waitingTimeTracker.end(parameters)
    }

    fun onStoreInstallationFailed() {
        abortMeasurement()
    }

    private fun abortMeasurement() {
        performanceTransactionId?.let {
            performanceTransactionRepository.finishTransaction(
                it,
                TransactionStatus.ABORTED
            )
        }
        performanceTransactionId = null
        waitingTimeTracker.abort()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_DESTROY -> {
                abortMeasurement()
            }

            else -> {
                // no-op
            }
        }
    }
}
