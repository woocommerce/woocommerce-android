package com.woocommerce.android.ui.connectivitytool.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.connectivitytool.FailureType
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
    private val isAppPasswordSite: Boolean
        get() = selectedSite.connectionType == SiteConnectionType.ApplicationPasswords

    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        val startTime = System.currentTimeMillis()
        val result = ssrFetcher.load(selectedSite.get())
        val durationMs = System.currentTimeMillis() - startTime
        if (result.isError) {
            emit(result.parseError(durationMs))
        } else {
            emit(Success(durationMs = durationMs))
        }
    }

    private fun WooResult<WCSSRModel>.parseError(durationMs: Long): Failure {
        val failureType = when (error.type) {
            WooErrorType.TIMEOUT -> FailureType.TIMEOUT
            WooErrorType.API_NOT_FOUND -> if (isAppPasswordSite) FailureType.GENERIC else FailureType.JETPACK
            WooErrorType.INVALID_RESPONSE -> FailureType.PARSE
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
        const val OPERATION_NAME = "Site Connection"
    }
}
