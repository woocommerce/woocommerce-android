package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.WooPushNotificationPreferencesEntity

@Dao
internal interface WooPushNotificationPreferencesDao {
    @Upsert
    suspend fun upsertPreferences(preferences: WooPushNotificationPreferencesEntity)

    @Query(
        """
            SELECT * FROM WooPushNotificationPreferences
            WHERE localSiteId = :localSiteId
        """
    )
    fun observePreferences(localSiteId: LocalId): Flow<WooPushNotificationPreferencesEntity?>
}
