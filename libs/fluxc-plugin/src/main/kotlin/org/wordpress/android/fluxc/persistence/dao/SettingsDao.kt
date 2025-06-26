package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.WCSettingsModel

@Dao
interface SettingsDao {
    @Query(
        """
        SELECT * FROM SettingsEntity
        WHERE localSiteId = :siteId
    """
    )
    suspend fun getSettings(siteId: LocalId): WCSettingsModel?

    @Upsert
    suspend fun upsertSettings(settings: WCSettingsModel)

    @Query("UPDATE SettingsEntity SET couponsEnabled = :couponsEnabled WHERE localSiteId = :siteId")
    suspend fun setCouponsEnabled(siteId: LocalId, couponsEnabled: Boolean)
}
