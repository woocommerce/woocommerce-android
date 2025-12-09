package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter

/**
 * TypeConverter for List<String> using ";@;" as separator.
 */
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
