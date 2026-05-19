package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(tableName = "SupportChatBookmarkEntity")
data class SupportChatBookmarkEntity(
    @PrimaryKey val chatId: Long,
    val localSiteId: LocalId,
    val remoteSiteId: Long,
    val botSlug: String,
    val sessionId: String?,
    @ColumnInfo(defaultValue = "0") val hasCreatedTicket: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isResolved: Boolean = false,
    val title: String?,
    val createdAt: Long,
    val updatedAt: Long
)
