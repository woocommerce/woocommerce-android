package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveStatsWidgetsStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore
) {
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .flatMapLatest { orderStore.observeOrderCountForSite(it) }
        .map { count -> count != 0 }
        .distinctUntilChanged()
        .transform { hasOrders ->
            if (!hasOrders) {
                val fetchResult = orderStore.fetchHasOrders(selectedSite.get(), null).let {
                    when (it) {
                        is HasOrdersResult.Success -> it.hasOrders
                        // Default to true if we can't determine if there are orders
                        is HasOrdersResult.Failure -> true
                    }
                }
                emit(fetchResult)
            } else {
                emit(true)
            }
        }.map { hasOrders ->
            if (hasOrders) {
                DashboardWidget.Status.Available
            } else {
                DashboardWidget.Status.Unavailable(
                    badgeText = R.string.my_store_widget_unavailable
                )
            }
        }
}
