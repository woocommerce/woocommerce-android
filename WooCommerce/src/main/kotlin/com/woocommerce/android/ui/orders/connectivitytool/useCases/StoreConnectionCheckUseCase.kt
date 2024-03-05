package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class StoreConnectionCheckUseCase @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {
    operator fun invoke(): Flow<ConnectivityTestStatus> = flow {
        emit(InProgress)
        wooCommerceStore.fetchSSR(selectedSite.get())
            .takeIf { it.isError }
            ?.let { emit(Failure) }
            ?: emit(Success)
    }
}
