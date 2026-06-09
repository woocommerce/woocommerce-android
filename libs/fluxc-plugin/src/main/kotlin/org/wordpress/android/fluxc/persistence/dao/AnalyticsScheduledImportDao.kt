package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.settings.AnalyticsScheduledImportSettingEntity

@Dao
interface AnalyticsScheduledImportDao {
    @Query("SELECT * FROM AnalyticsScheduledImportSetting WHERE localSiteId = :localSiteId")
    fun observeSetting(localSiteId: LocalOrRemoteId.LocalId): Flow<AnalyticsScheduledImportSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(setting: AnalyticsScheduledImportSettingEntity): Long
}
