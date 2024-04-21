package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class DashboardRepository @Inject constructor(
    selectedSite: SelectedSite,
    private val dashboardDataStore: DashboardDataStore,
    orderStore: WCOrderStore,
    private val dispatchers: CoroutineDispatchers
) {
    private val statsWidgetsAvailability = orderStore
        .observeOrderCountForSite(selectedSite.get())
        .map { count -> count != 0 }

    val widgets = combine(
        dashboardDataStore.dashboard.map { it.widgetsList },
        statsWidgetsAvailability
    ) { widgets, hasOrders ->
        widgets.toDomainModel(hasOrders)
    }

    suspend fun updateWidgets(widgets: List<DashboardWidget>) = withContext(dispatchers.io) {
        runCatching {
            dashboardDataStore.updateDashboard(
                DashboardDataModel.newBuilder()
                    .addAllWidgets(widgets.map { it.toDataModel() })
                    .build()
            )
        }.onFailure { throwable ->
            WooLog.e(WooLog.T.DASHBOARD, "Failed to update dashboard data", throwable)
        }
    }

    suspend fun updateWidgetVisibility(type: DashboardWidget.Type, isVisible: Boolean) = withContext(dispatchers.io) {
        val dataStoreWidgets = widgets.first()
            .toMutableList()
            .apply {
                val index = indexOfFirst { it.type == type }
                if (index != -1) {
                    set(index, get(index).copy(isVisible = isVisible))
                }
            }
        updateWidgets(dataStoreWidgets)
    }

    private fun List<DashboardWidgetDataModel>.toDomainModel(
        statsWidgetsAvailability: Boolean
    ): List<DashboardWidget> {
        return map { widget ->
            val type = DashboardWidget.Type.valueOf(widget.type)
            DashboardWidget(
                type = type,
                isVisible = widget.isAdded,
                isAvailable = when (type) {
                    DashboardWidget.Type.STATS,
                    DashboardWidget.Type.POPULAR_PRODUCTS -> statsWidgetsAvailability

                    else -> true
                }
            )
        }
    }
}
