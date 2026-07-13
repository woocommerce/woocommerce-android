package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import java.time.DayOfWeek

@Entity(tableName = "WPSiteSettings")
data class WPSiteSettingsModel(
    @PrimaryKey val localSiteId: LocalId,
    val startOfWeek: DayOfWeek?
)
