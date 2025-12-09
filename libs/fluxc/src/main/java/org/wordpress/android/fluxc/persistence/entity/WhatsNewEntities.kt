package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(tableName = "WhatsNewAnnouncementEntity")
data class WhatsNewAnnouncementEntity(
    @androidx.room.PrimaryKey
    val announcementId: RemoteId,
    val minimumAppVersion: String,
    val maximumAppVersion: String,
    val appVersionTargets: List<String>,
    val localized: Boolean
)

@Entity(
    tableName = "WhatsNewAnnouncementFeatureEntity",
    primaryKeys = ["announcementId", "title"],
    foreignKeys = [
        ForeignKey(
            entity = WhatsNewAnnouncementEntity::class,
            parentColumns = ["announcementId"],
            childColumns = ["announcementId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WhatsNewAnnouncementFeatureEntity(
    val announcementId: RemoteId,
    val title: String,
    val subtitle: String?,
    val iconUrl: String?,
    val iconBase64: String?
)
