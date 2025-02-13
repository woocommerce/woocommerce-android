package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import java.math.BigDecimal

class BigDecimalConverter {
    @TypeConverter
    fun bigDecimalToString(input: BigDecimal): String = input.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(input: String): BigDecimal = input.toBigDecimalOrNull() ?: BigDecimal.ZERO
}
