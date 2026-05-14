package com.woocommerce.android.ui.dashboard.data

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import com.woocommerce.android.di.SiteComponentEntryPoint
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.EntryPoints
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardDataStore @Inject constructor(
    private val selectedSite: SelectedSite
) {
    private val dataStoreFlow: Flow<DataStore<DashboardDataModel>?> = selectedSite.observe().map {
        selectedSite.siteComponent?.let { component ->
            EntryPoints.get(component, SiteComponentEntryPoint::class.java).dashboardDataStore()
        }
    }

    val widgets: Flow<List<DashboardWidgetDataModel>> = dataStoreFlow.flatMapLatest { dataStore ->
        val flow = dataStore?.data ?: flow {
            WooLog.e(T.DASHBOARD, "DashboardDataStore: Site component is null, providing default widgets.")
            emit(DashboardDataModel.getDefaultInstance())
        }
        flow.catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                WooLog.e(T.DASHBOARD, "Error reading dashboard data.", exception)
                emit(DashboardDataModel.getDefaultInstance())
            } else {
                throw exception
            }
        }.map {
            if (it == DashboardDataModel.getDefaultInstance()) {
                DashboardDataModel.newBuilder().addAllWidgets(getDefaultWidgets()).build()
            } else {
                it
            }
        }.map {
            it.widgetsList.filter { widget ->
                DashboardWidget.Type.supportedWidgets.any { type -> type.name == widget.type }
            }
        }
    }

    suspend fun updateDashboard(dashboard: DashboardDataModel) {
        val dataStore = dataStoreFlow.first()
        if (dataStore == null) {
            WooLog.e(T.DASHBOARD, "Cannot update dashboard data: Site component is null")
        } else {
            runCatching {
                dataStore.updateData { dashboard }
            }.onFailure {
                WooLog.e(T.DASHBOARD, "Failed to update dashboard data")
            }
        }
    }

    @VisibleForTesting
    internal fun getDefaultWidgets(): List<DashboardWidgetDataModel> {
        fun DashboardWidget.Type.shouldBeEnabledByDefault() =
            this == DashboardWidget.Type.AI_ASSISTANT ||
                this == DashboardWidget.Type.STATS ||
                this == DashboardWidget.Type.POPULAR_PRODUCTS ||
                this == DashboardWidget.Type.ONBOARDING ||
                this == DashboardWidget.Type.BLAZE ||
                this == DashboardWidget.Type.GOOGLE_ADS ||
                this == DashboardWidget.Type.PUSH_NOTIFICATIONS

        return DashboardWidget.Type.supportedWidgets.map {
            DashboardWidgetDataModel.newBuilder()
                .setType(it.name)
                .setIsAdded(it.shouldBeEnabledByDefault())
                .build()
        }
    }
}
