package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.persistence.entity.AppVersionTargets

class AppVersionTargetsConverter {
    @TypeConverter
    fun fromAppVersionTargets(value: AppVersionTargets?): String? {
        return value?.value?.joinToString(separator = SEPARATOR)
    }

    @TypeConverter
    fun toAppVersionTargets(value: String?): AppVersionTargets? {
        return value?.split(SEPARATOR)?.filter { it.isNotEmpty() }?.let { AppVersionTargets(it) }
    }

    companion object {
        private const val SEPARATOR = ";@;"
    }
}
