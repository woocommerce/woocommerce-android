package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.orders.connectivitytool.FailureType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class StoreConnectionCheckUseCase @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        wooCommerceStore.fetchSSR(selectedSite.get())
            .takeIf { it.isError }
            ?.parseError()
            ?.let { emit(it) }
            ?: emit(Success)
    }

    private fun WooResult<WCSSRModel>.parseError() =
        when (error.type) {
            WooErrorType.TIMEOUT -> Failure(FailureType.TIMEOUT)
            WooErrorType.API_NOT_FOUND -> Failure(FailureType.JETPACK)
            WooErrorType.INVALID_RESPONSE -> Failure(FailureType.PARSE)
            else -> Failure(FailureType.GENERIC)
        }
}
