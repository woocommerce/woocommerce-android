package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.WPSiteSettingsModel

@Dao
abstract class WPSiteSettingsDao {
    @Query("SELECT * FROM WPSiteSettings WHERE localSiteId = :siteId LIMIT 1")
    abstract suspend fun getSiteSettings(siteId: LocalId): WPSiteSettingsModel?

    @Upsert
    abstract suspend fun upsertSiteSettings(model: WPSiteSettingsModel)
}
