package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.observeEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSiteOrdersState @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val dispatcher: Dispatcher
) {
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .flatMapLatest { observeHasOrdersFromOrdersStatusOptions(it) }
        .map {
            // Fallback to fetching from API if we can't infer the value from the order status options
            it ?: fetchHasOrdersFromApi()
        }
        .map { hasOrders ->
            if (hasOrders) {
                DashboardWidget.Status.Available
            } else {
                DashboardWidget.Status.Unavailable(
                    badgeText = R.string.my_store_widget_unavailable
                )
            }
        }

    private fun observeHasOrdersFromOrdersStatusOptions(site: SiteModel) =
        dispatcher.observeEvents<OnOrderStatusOptionsChanged>()
            .onStart { emit(OnOrderStatusOptionsChanged(0)) }
            .map {
                orderStore.getOrderStatusOptionsForSite(site)
                    .filter { it.statusKey != "checkout-draft" }
                    .takeIf { it.isNotEmpty() }
                    ?.any { it.statusCount > 0 }
            }
            .distinctUntilChanged()
            .flowOn(coroutineDispatchers.io)

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
