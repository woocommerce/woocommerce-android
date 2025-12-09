package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

class RemoteIdConverter {
    @TypeConverter
    fun fromRemoteId(value: RemoteId?): Long? = value?.value

    @TypeConverter
    fun toRemoteId(value: Long?): RemoteId? = value?.let { RemoteId(it) }
}
