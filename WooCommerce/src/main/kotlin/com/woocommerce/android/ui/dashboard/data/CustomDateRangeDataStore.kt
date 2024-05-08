package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Date

abstract class CustomDateRangeDataStore(
    private val dataStore: DataStore<CustomDateRange>
) {
    val dateRange: Flow<StatsTimeRange?> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                WooLog.e(WooLog.T.DASHBOARD, "Error reading custom date range preferences.", exception)
                emit(CustomDateRange.getDefaultInstance())
            } else {
                throw exception
            }
        }
        .map {
            if (it == CustomDateRange.getDefaultInstance()) {
                null
            } else {
                StatsTimeRange(Date(it.startDateMillis), Date(it.endDateMillis))
            }
        }

    suspend fun updateDateRange(range: StatsTimeRange) {
        dataStore.updateData { preferences ->
            preferences.toBuilder()
                .setStartDateMillis(range.start.time)
                .setEndDateMillis(range.end.time)
                .build()
        }
    }
}
