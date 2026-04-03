package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.troubleshooting.FailureType
import com.woocommerce.android.util.WCSSRModelCachingFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import javax.inject.Inject

class StoreConnectionCheckUseCase @Inject constructor(
    private val selectedSite: SelectedSite,
    private val ssrFetcher: WCSSRModelCachingFetcher
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        val startTime = System.currentTimeMillis()
        val result = ssrFetcher.load(selectedSite.get())
        val durationMs = System.currentTimeMillis() - startTime
        val isAppPassword = selectedSite.get().connectionType == SiteConnectionType.ApplicationPasswords
        if (result.isError) {
            emit(result.parseError(durationMs, isAppPassword))
        } else {
            emit(Success(durationMs = durationMs))
        }
    }

    private fun WooResult<WCSSRModel>.parseError(durationMs: Long, isAppPasswordSite: Boolean): Failure {
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
        const val OPERATION_NAME = "Connecting to your site"
    }
}
