package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.model.toWidgetModelList
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DashboardRepository @Inject constructor(
    private val dashboardDataStore: DashboardDataStore,
    private val dispatchers: CoroutineDispatchers
) {
    val widgets = dashboardDataStore.dashboard
        .map { it.toWidgetModelList() }
        .flowOn(dispatchers.io)

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
}
