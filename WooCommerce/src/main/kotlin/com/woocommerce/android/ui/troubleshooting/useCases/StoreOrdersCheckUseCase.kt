package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.troubleshooting.FailureType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import javax.inject.Inject
import kotlin.time.measureTimedValue

class StoreOrdersCheckUseCase @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        val site = selectedSite.get()
        val (result, duration) = measureTimedValue { orderStore.fetchHasOrders(site, null) }

        val failure = (result as? HasOrdersResult.Failure)?.let {
            val isAppPassword = site.connectionType == SiteConnectionType.ApplicationPasswords
            it.parseError(duration.inWholeMilliseconds, isAppPassword)
        }
        if (failure != null) {
            emit(failure)
        } else {
            emit(Success(durationMs = duration.inWholeMilliseconds))
        }
    }

    private fun HasOrdersResult.Failure.parseError(durationMs: Long, isAppPasswordSite: Boolean): Failure {
        val failureType = when {
            !isAppPasswordSite && error.isJetpackNotConnectedError() -> FailureType.JETPACK
            error.type == OrderErrorType.TIMEOUT_ERROR -> FailureType.TIMEOUT
            error.type == OrderErrorType.PARSE_ERROR -> FailureType.PARSE
            else -> FailureType.GENERIC
        }

        return Failure(
            error = failureType,
            technicalDetails = formatErrorDetails(
                operation = OPERATION_NAME,
                errorType = error.type.name,
                message = error.message
            ),
            durationMs = durationMs
        )
    }

    companion object {
        const val OPERATION_NAME = "Fetching your site orders"
    }
}
