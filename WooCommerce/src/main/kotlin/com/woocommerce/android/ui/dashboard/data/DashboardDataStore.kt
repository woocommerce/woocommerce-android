package com.woocommerce.android.ui.dashboard.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardDataStore @Inject constructor(
    private val appContext: Context,
    selectedSite: SelectedSite,
    private val crashLogging: CrashLogging,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers
) {
    private val dataStores = mutableMapOf<Int, DataStore<DashboardDataModel>>()

    private val selectedSiteIdFlow = selectedSite.observe().map { it?.id }.distinctUntilChanged()

    val widgets: Flow<List<DashboardWidgetDataModel>> = selectedSiteIdFlow.flatMapLatest { localSiteId ->
        val flow = localSiteId?.let { dataStoreFor(it).data } ?: flow {
            WooLog.e(T.DASHBOARD, "DashboardDataStore: No selected site, providing default widgets.")
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
        val localSiteId = selectedSiteIdFlow.first()
        if (localSiteId == null) {
            WooLog.e(T.DASHBOARD, "Cannot update dashboard data: No selected site")
        } else {
            runCatching {
                dataStoreFor(localSiteId).updateData { dashboard }
            }.onFailure {
                WooLog.e(T.DASHBOARD, "Failed to update dashboard data")
            }
        }
    }

    private fun dataStoreFor(localSiteId: Int): DataStore<DashboardDataModel> = synchronized(dataStores) {
        dataStores.getOrPut(localSiteId) {
            DataStoreFactory.create(
                produceFile = { appContext.dataStoreFile("dashboard_configuration_$localSiteId") },
                corruptionHandler = ReplaceFileCorruptionHandler {
                    crashLogging.recordEvent("Corrupted data store. DataStore Type: DASHBOARD")
                    DashboardDataModel.getDefaultInstance()
                },
                scope = CoroutineScope(appCoroutineScope.coroutineContext + dispatchers.io),
                serializer = DashboardSerializer
            )
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
