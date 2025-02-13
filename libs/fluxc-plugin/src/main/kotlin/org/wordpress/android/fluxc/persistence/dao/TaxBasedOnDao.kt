package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.taxes.TaxBasedOnSettingEntity

@Dao
interface TaxBasedOnDao {
    @Query("SELECT * FROM TaxBasedOnSetting WHERE localSiteId = :localSiteId")
    suspend fun getTaxBasedOnSetting(localSiteId: LocalOrRemoteId.LocalId): TaxBasedOnSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(taxBasedOnSetting: TaxBasedOnSettingEntity): Long
}
