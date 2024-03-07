package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
import javax.inject.Inject

class StoreOrdersCheckUseCase @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)
        orderStore.fetchHasOrders(
            site = selectedSite.get(),
            status = null
        ).takeIf { it is HasOrdersResult.Success }?.let {
            emit(Success)
        } ?: emit(Failure())
    }
}
