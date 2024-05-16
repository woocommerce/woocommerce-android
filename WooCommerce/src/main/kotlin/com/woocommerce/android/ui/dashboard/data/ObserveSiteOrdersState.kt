package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSiteOrdersState @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .flatMapLatest { orderStore.observeOrderCountForSite(it) }
        .map { count -> count != 0 }
        .distinctUntilChanged()
        .transformLatest { hasOrders ->
            if (!hasOrders) {
                // This means either the store doesn't have orders, or no orders are cached yet
                // Use other approaches to determine if the store has orders
                emit(getHasOrdersFromOrderStatusOptions() ?: fetchHasOrdersFromApi())
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

    private suspend fun getHasOrdersFromOrderStatusOptions() = withContext(coroutineDispatchers.io) {
        orderStore.getOrderStatusOptionsForSite(selectedSite.get())
            .filter { it.statusKey != "checkout-draft" }
            .takeIf { it.isNotEmpty() }
            ?.sumOf { it.statusCount }
            ?.let { it != 0 }
    }

    private suspend fun fetchHasOrdersFromApi(): Boolean {
        return orderStore.fetchHasOrders(selectedSite.get(), null).let {
            when (it) {
                is HasOrdersResult.Success -> it.hasOrders
                // Default to true if we can't determine if there are orders
                is HasOrdersResult.Failure -> true
            }
        }
    }
}
