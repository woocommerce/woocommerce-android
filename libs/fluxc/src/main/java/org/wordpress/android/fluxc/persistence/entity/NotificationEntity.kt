package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

/**
 * Room entity for notifications.
 *
 * Uses a composite primary key of [remoteSiteId] and [remoteNoteId] since notifications
 * are uniquely identified by the combination of the site they belong to and their remote ID.
 */
@Entity(
    tableName = "NotificationEntity",
    primaryKeys = ["remoteSiteId", "remoteNoteId"]
)
data class NotificationEntity(
    val remoteSiteId: RemoteId,
    val remoteNoteId: RemoteId,
    val noteHash: Long,
    val type: String,
    val subtype: String?,
    val read: Boolean,
    val icon: String?,
    val noticon: String?,
    val timestamp: String?,
    val url: String?,
    val title: String?,
    val formattableBody: String?,
    val formattableSubject: String?,
    val formattableMeta: String?
)
