package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class InternetConnectionTestUseCase @Inject constructor(
    private val networkStatus: NetworkStatus
) {
    operator fun invoke(): Flow<ConnectivityTestStatus> = flow {
        emit(ConnectivityTestStatus.InProgress)
        if (networkStatus.isConnected()) {
            emit(ConnectivityTestStatus.Success)
        } else {
            emit(ConnectivityTestStatus.Failure)
        }
    }
}
