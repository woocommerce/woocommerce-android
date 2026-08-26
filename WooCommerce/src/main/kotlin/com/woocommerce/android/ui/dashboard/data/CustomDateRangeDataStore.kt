package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.extensions.toDateAtStartOfDay
import com.woocommerce.android.extensions.toEpochDay
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import com.woocommerce.android.util.WooLog
import com.woocommerce.commons.stats.StatsTimeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

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
            // Day 0 means no saved range
            if (it.startDateEpochDay == 0L) {
                null
            } else {
                StatsTimeRange(it.startDateEpochDay.toDateAtStartOfDay(), it.endDateEpochDay.toDateAtStartOfDay())
            }
        }

    suspend fun updateDateRange(range: StatsTimeRange) {
        dataStore.updateData { preferences ->
            preferences.toBuilder()
                .setStartDateEpochDay(range.start.toEpochDay())
                .setEndDateEpochDay(range.end.toEpochDay())
                .build()
        }
    }
}
