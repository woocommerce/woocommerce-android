package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Relation
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

data class AppVersionTargets(val value: List<String>)

@Entity(tableName = "WhatsNewAnnouncementEntity")
data class WhatsNewAnnouncementEntity(
    @androidx.room.PrimaryKey
    val announcementId: RemoteId,
    val minimumAppVersion: String,
    val maximumAppVersion: String,
    val appVersionTargets: AppVersionTargets,
    @ColumnInfo(defaultValue = "")
    val detailsUrl: String = "",
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

/**
 * Room relation class that automatically joins announcements with their features.
 */
data class WhatsNewAnnouncementWithFeatures(
    @Embedded
    val announcement: WhatsNewAnnouncementEntity,

    @Relation(
        parentColumn = "announcementId",
        entityColumn = "announcementId"
    )
    val features: List<WhatsNewAnnouncementFeatureEntity>
)
