package com.woocommerce.android.ui.connectivitytool.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.connectivitytool.FailureType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.PLUGIN_NOT_ACTIVE
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.TIMEOUT_ERROR
import javax.inject.Inject

class StoreOrdersCheckUseCase @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) {
    private val isAppPasswordSite: Boolean
        get() = selectedSite.connectionType == SiteConnectionType.ApplicationPasswords

    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        orderStore.fetchHasOrders(selectedSite.get(), null)
            .run { this as? HasOrdersResult.Failure }
            ?.parseError()
            ?.let { emit(it) }
            ?: emit(Success)
    }

    private fun HasOrdersResult.Failure.parseError(): Failure {
        val failureType = when (error.type) {
            TIMEOUT_ERROR -> FailureType.TIMEOUT
            PARSE_ERROR -> FailureType.PARSE
            PLUGIN_NOT_ACTIVE -> if (isAppPasswordSite) FailureType.GENERIC else FailureType.JETPACK
            else -> FailureType.GENERIC
        }

        return Failure(
            error = failureType,
            technicalDetails = formatErrorDetails(
                operation = OPERATION_NAME,
                errorType = error.type.name,
                message = error.message
            )
        )
    }

    companion object {
        const val OPERATION_NAME = "Fetch Orders"
    }
}
