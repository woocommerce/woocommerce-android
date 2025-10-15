package org.wordpress.android.fluxc.persistence.converters

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@ProvidedTypeConverter
class StatsGranularityConverter @Inject constructor(
    private val logger: AppLogWrapper
) {
    @TypeConverter
    fun fromStatsGranularity(value: StatsGranularity): String {
        return value.name
    }

    @TypeConverter
    fun toStatsGranularity(value: String): StatsGranularity {
        return try {
            StatsGranularity.valueOf(value.uppercase())
        } catch (e: IllegalArgumentException) {
            logger.e(AppLog.T.DB, "Error converting $value", e)
            StatsGranularity.HOURS
        }
    }
}
