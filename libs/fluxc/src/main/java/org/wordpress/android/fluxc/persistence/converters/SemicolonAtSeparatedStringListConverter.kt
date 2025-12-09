package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter

class SemicolonAtSeparatedStringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(separator = SEPARATOR)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(SEPARATOR)?.filter { it.isNotEmpty() }
    }

    companion object {
        private const val SEPARATOR = ";@;"
    }
}
