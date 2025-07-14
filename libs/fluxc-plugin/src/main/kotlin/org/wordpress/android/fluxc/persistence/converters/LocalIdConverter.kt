package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

class LocalIdConverter {
    @TypeConverter
    fun fromLocalId(value: LocalId?) = value?.value

    @TypeConverter
    fun toLocalId(value: Int?) = value?.let { LocalId(it) }
}
