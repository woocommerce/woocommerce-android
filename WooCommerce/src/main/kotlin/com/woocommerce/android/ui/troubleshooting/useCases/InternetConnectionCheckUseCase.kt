package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.time.measureTimedValue

class InternetConnectionCheckUseCase @Inject constructor(
    private val networkStatus: NetworkStatus
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        val (isConnected, duration) = measureTimedValue { networkStatus.isConnected() }

        if (isConnected) {
            emit(Success(durationMs = duration.inWholeMilliseconds))
        } else {
            emit(Failure(durationMs = duration.inWholeMilliseconds))
        }
    }

    companion object {
        const val OPERATION_NAME = "Internet Connection"
    }
}
