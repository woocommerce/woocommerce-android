package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.WCSettingsModel

@Dao
internal interface SettingsDao {
    @Query(
        """
        SELECT * FROM SettingsEntity
        WHERE localSiteId = :siteId
    """
    )
    suspend fun getSettings(siteId: LocalId): WCSettingsModel?

    @Query(
        """
        SELECT * FROM SettingsEntity
    """
    )
    fun observeAllSettings(): Flow<List<WCSettingsModel>>

    @Upsert
    suspend fun upsertSettings(settings: WCSettingsModel)

    @Query("UPDATE SettingsEntity SET couponsEnabled = :couponsEnabled WHERE localSiteId = :siteId")
    suspend fun setCouponsEnabled(siteId: LocalId, couponsEnabled: Boolean)
}
