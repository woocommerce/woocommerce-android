package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.TypeConverters
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.converters.ISO8601DateConverter
import java.util.Date

@Entity(tableName = "OrderNotes", primaryKeys = ["localSiteId", "noteId"])
data class OrderNoteEntity(
    val localSiteId: LocalId,
    val noteId: RemoteId,
    val orderId: RemoteId,
    @field:TypeConverters(ISO8601DateConverter::class) val dateCreated: Date? = null,
    val note: String? = null,
    val author: String? = null,
    val isSystemNote: Boolean, // True if the note is 'system-created', else created by a site user
    val isCustomerNote: Boolean // False if private, else customer-facing.
)
