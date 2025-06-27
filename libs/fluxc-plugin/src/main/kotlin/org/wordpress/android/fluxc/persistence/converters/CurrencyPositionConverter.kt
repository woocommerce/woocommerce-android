package org.wordpress.android.fluxc.persistence.converters

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@ProvidedTypeConverter
class CurrencyPositionConverter @Inject constructor(
    private val logger: AppLogWrapper
) {

    @TypeConverter
    fun fromCurrencyPosition(value: CurrencyPosition): String {
        return value.name
    }

    @TypeConverter
    fun toCurrencyPosition(value: String): CurrencyPosition {
        return try {
            CurrencyPosition.valueOf(value.uppercase())
        } catch (e: IllegalArgumentException) {
            logger.e(AppLog.T.DB, "Error converting $value", e)
            CurrencyPosition.LEFT
        }
    }
}
