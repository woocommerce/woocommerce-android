package org.wordpress.android.fluxc.model.settings

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(tableName = "AnalyticsScheduledImportSetting")
data class AnalyticsScheduledImportSettingEntity(
    @PrimaryKey val localSiteId: LocalId,
    val isEnabled: Boolean,
)
