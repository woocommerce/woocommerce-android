package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.WCSettingsEntity

@Dao
interface SettingsDao {
    @Query(
        """
        SELECT * FROM WCSettingsEntity
        WHERE localSiteId = :siteId
        LIMIT 1
    """
    )
    suspend fun getSettingsForSite(siteId: LocalId): WCSettingsEntity?

    @Upsert
    suspend fun insertOrUpdateSettings(settings: WCSettingsEntity)

    @Query("UPDATE WCSettingsEntity SET couponsEnabled = :couponsEnabled WHERE localSiteId = :siteId")
    suspend fun setCouponsEnabled(siteId: LocalId, couponsEnabled: Boolean): Int
}
