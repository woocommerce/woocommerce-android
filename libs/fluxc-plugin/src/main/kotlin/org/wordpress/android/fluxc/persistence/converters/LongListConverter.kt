package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter

class LongListConverter {
    companion object {
        private const val SEPARATOR = ","
    }

    @TypeConverter
    fun listToString(value: List<Long>?): String? =
        value?.joinToString(separator = SEPARATOR)

    @TypeConverter
    fun stringToList(value: String?): List<Long>? = value?.let {
        if (it.isEmpty()) {
            emptyList()
        } else {
            it.split(SEPARATOR).map(String::toLong)
        }
    }
}
