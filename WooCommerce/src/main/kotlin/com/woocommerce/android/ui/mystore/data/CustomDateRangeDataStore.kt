package com.woocommerce.android.ui.mystore.data

import androidx.datastore.core.DataStore
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Date
import javax.inject.Inject

class CustomDateRangeDataStore @Inject constructor(
    private val dataStore: DataStore<CustomDateRange>
) {
    val dateRange: Flow<DateRange?> = dataStore.data
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
                DateRange(Date(it.startDateMillis), Date(it.endDateMillis))
            }
        }

    suspend fun updateDateRange(startDateMillis: Long, endDateMillis: Long) {
        dataStore.updateData { preferences ->
            preferences.toBuilder()
                .setStartDateMillis(startDateMillis)
                .setEndDateMillis(endDateMillis)
                .build()
        }
    }
}
