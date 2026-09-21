package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.settings.SubscriptionProductCreationSettingsEntity

@Dao
interface SubscriptionProductCreationSettingsDao {
    @Query("SELECT * FROM SubscriptionProductCreationSettings WHERE localSiteId = :localSiteId")
    suspend fun getSettings(localSiteId: LocalOrRemoteId.LocalId): SubscriptionProductCreationSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: SubscriptionProductCreationSettingsEntity): Long
}
