package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter

class StringListConverter {
    companion object {
        private const val SEPARATOR = ","
    }

    @TypeConverter
    fun listToString(value: List<String>?): String? =
        value?.joinToString(separator = SEPARATOR)

    @TypeConverter
    fun stringToList(value: String?): List<String>? = value?.let {
        if (it.isEmpty()) {
            emptyList()
        } else {
            it.split(SEPARATOR)
        }
    }
}
