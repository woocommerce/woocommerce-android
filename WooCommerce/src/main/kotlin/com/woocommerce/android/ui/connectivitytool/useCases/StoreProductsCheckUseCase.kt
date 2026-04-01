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
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class StoreProductsCheckUseCase @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    private val isAppPasswordSite: Boolean
        get() = selectedSite.connectionType == SiteConnectionType.ApplicationPasswords

    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        productStore.fetchProducts(selectedSite.get())
            .takeIf { it.isError }
            ?.parseError()
            ?.let { emit(it) }
            ?: emit(Success)
    }

    private fun WooResult<List<WCProductModel>>.parseError(): Failure {
        val failureType = when (error.type) {
            WooErrorType.TIMEOUT -> FailureType.TIMEOUT
            WooErrorType.INVALID_RESPONSE -> FailureType.PARSE
            WooErrorType.API_NOT_FOUND -> if (isAppPasswordSite) FailureType.GENERIC else FailureType.JETPACK
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
        const val OPERATION_NAME = "Fetch Products"
    }
}
