package com.woocommerce.android.ui.mystore.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class DashboardDataStore @Inject constructor(
    private val dataStore: DataStore<DashboardDataModel>
) {
    val dashboard: Flow<DashboardDataModel?> = dataStore.data
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
                null
            } else {
                it
            }
        }

    suspend fun updateDashboard(dashboard: DashboardDataModel) {
        runCatching {
            dataStore.updateData { dashboard }
        }.onFailure {
            WooLog.e(T.DASHBOARD, "Failed to update dashboard data")
        }
    }
}
