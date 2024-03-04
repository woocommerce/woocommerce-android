package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StoreOrdersTestUseCase {
    operator fun invoke(): Flow<ConnectivityTestStatus> = flow {
        emit(ConnectivityTestStatus.InProgress)
        emit(ConnectivityTestStatus.Success)
    }
}
