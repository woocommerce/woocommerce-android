package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.DataMigration
import com.woocommerce.android.extensions.toEpochDay
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import java.util.Date

// Migrates ranges saved before 25.5 from the millis fields. Delete in 25.9, see WOOMOB-3841.
object CustomDateRangeDayMigration : DataMigration<CustomDateRange> {
    override suspend fun shouldMigrate(currentData: CustomDateRange): Boolean =
        currentData.startDateMillis != 0L || currentData.endDateMillis != 0L

    override suspend fun migrate(currentData: CustomDateRange): CustomDateRange =
        currentData.toBuilder()
            .apply {
                if (startDateEpochDay == 0L && endDateEpochDay == 0L) {
                    setStartDateEpochDay(Date(currentData.startDateMillis).toEpochDay())
                    setEndDateEpochDay(Date(currentData.endDateMillis).toEpochDay())
                }
            }
            .clearStartDateMillis()
            .clearEndDateMillis()
            .build()

    override suspend fun cleanUp() = Unit
}
