package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(tableName = "SupportChatBookmarkEntity")
data class SupportChatBookmarkEntity(
    @PrimaryKey val chatId: Long,
    val localSiteId: LocalId,
    val remoteSiteId: Long,
    val botSlug: String,
    val title: String?,
    val createdAt: Long,
    val updatedAt: Long
)
