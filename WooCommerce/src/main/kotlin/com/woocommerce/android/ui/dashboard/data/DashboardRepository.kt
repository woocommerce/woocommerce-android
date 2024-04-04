package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.model.toWidgetModelList
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DashboardRepository @Inject constructor(
    private val dashboardDataStore: DashboardDataStore,
    private val dispatchers: CoroutineDispatchers
) {
    private val defaultWidgets = listOf(
        DashboardWidget(type = DashboardWidget.Type.ONBOARDING, isAdded = true),
        DashboardWidget(type = DashboardWidget.Type.STATS, isAdded = true),
        DashboardWidget(type = DashboardWidget.Type.POPULAR_PRODUCTS, isAdded = true),
        DashboardWidget(type = DashboardWidget.Type.BLAZE, isAdded = true)
    )

    val widgets = dashboardDataStore.dashboard
        .map { it?.toWidgetModelList() ?: defaultWidgets }
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
}
