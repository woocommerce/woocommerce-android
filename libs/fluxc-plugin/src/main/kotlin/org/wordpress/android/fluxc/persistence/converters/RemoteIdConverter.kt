package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

class RemoteIdConverter {
    @TypeConverter
    fun fromRemoteId(value: RemoteId?) = value?.value

    @TypeConverter
    fun toRemoteId(value: Long?) = value?.let { RemoteId(it) }
}
