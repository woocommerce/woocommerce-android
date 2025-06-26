package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.model.settings.CurrencyPosition

class CurrencyPositionConverter {

    @TypeConverter
    fun fromCurrencyPosition(value: CurrencyPosition): String {
        return value.name
    }

    @TypeConverter
    fun toCurrencyPosition(value: String): CurrencyPosition {
        return CurrencyPosition.valueOf(value.uppercase())
    }
}
