package com.woocommerce.android.ui.connectivitytool.useCases

import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class InternetConnectionCheckUseCase @Inject constructor(
    private val networkStatus: NetworkStatus
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        val startTime = System.currentTimeMillis()
        val isConnected = networkStatus.isConnected()
        val durationMs = System.currentTimeMillis() - startTime
        if (isConnected) {
            emit(Success(durationMs = durationMs))
        } else {
            emit(Failure(durationMs = durationMs))
        }
    }

    companion object {
        const val OPERATION_NAME = "Internet Connection"
    }
}
