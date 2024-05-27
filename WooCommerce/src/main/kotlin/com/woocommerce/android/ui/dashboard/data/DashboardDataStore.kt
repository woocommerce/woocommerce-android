package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.di.SiteComponentEntryPoint
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.EntryPoints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class DashboardDataStore @Inject constructor(
    selectedSite: SelectedSite
) {
    private val dataStore: DataStore<DashboardDataModel> = EntryPoints.get(
        selectedSite.siteComponent!!,
        SiteComponentEntryPoint::class.java
    ).dashboardDataStore()

    val widgets: Flow<List<DashboardWidgetDataModel>> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                WooLog.e(T.DASHBOARD, "Error reading dashboard data.", exception)
                emit(DashboardDataModel.getDefaultInstance())
            } else {
                throw exception
            }
        }
        .map {
            if (it == DashboardDataModel.getDefaultInstance()) {
                DashboardDataModel.newBuilder().addAllWidgets(getDefaultWidgets()).build()
            } else {
                it
            }
        }.map { it.widgetsList }

    suspend fun updateDashboard(dashboard: DashboardDataModel) {
        runCatching {
            dataStore.updateData { dashboard }
        }.onFailure {
            WooLog.e(T.DASHBOARD, "Failed to update dashboard data")
        }
    }

    private fun getDefaultWidgets(): List<DashboardWidgetDataModel> {
        fun DashboardWidget.Type.shouldBeEnabledByDefault() =
            this == DashboardWidget.Type.STATS ||
                this == DashboardWidget.Type.POPULAR_PRODUCTS ||
                this == DashboardWidget.Type.ONBOARDING ||
                this == DashboardWidget.Type.BLAZE

        return DashboardWidget.Type.supportedWidgets.map {
            DashboardWidgetDataModel.newBuilder()
                .setType(it.name)
                .setIsAdded(it.shouldBeEnabledByDefault())
                .build()
        }
    }
}
