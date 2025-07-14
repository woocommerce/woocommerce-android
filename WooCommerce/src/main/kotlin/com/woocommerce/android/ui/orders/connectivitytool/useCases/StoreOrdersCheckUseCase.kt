package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.orders.connectivitytool.FailureType
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
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        orderStore.fetchHasOrders(selectedSite.get(), null)
            .run { this as? HasOrdersResult.Failure }
            ?.parseError()
            ?.let { emit(it) }
            ?: emit(Success)
    }

    private fun HasOrdersResult.Failure.parseError() =
        when (error.type) {
            TIMEOUT_ERROR -> Failure(FailureType.TIMEOUT)
            PARSE_ERROR -> Failure(FailureType.PARSE)
            PLUGIN_NOT_ACTIVE -> Failure(FailureType.JETPACK)
            else -> Failure(FailureType.GENERIC)
        }
}
