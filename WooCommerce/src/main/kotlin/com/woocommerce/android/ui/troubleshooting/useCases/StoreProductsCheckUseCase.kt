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
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import kotlin.time.measureTimedValue

class StoreProductsCheckUseCase @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        val site = selectedSite.get()
        val (result, duration) = measureTimedValue { productStore.fetchProducts(site) }

        if (result.isError) {
            val isAppPassword = site.connectionType == SiteConnectionType.ApplicationPasswords
            emit(result.parseError(duration.inWholeMilliseconds, isAppPassword))
        } else {
            emit(Success(durationMs = duration.inWholeMilliseconds))
        }
    }

    private fun WooResult<List<WCProductModel>>.parseError(durationMs: Long, isAppPasswordSite: Boolean): Failure {
        val failureType = when {
            !isAppPasswordSite && error.isJetpackNotConnectedError() -> FailureType.JETPACK
            error.type == WooErrorType.TIMEOUT -> FailureType.TIMEOUT
            error.type == WooErrorType.INVALID_RESPONSE -> FailureType.PARSE
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
        const val OPERATION_NAME = "Fetching products in your store"
    }
}
