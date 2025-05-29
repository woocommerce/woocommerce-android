package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCProductSettingsModel

@Dao
abstract class ProductSettingsDao {

    @Upsert
    abstract suspend fun upsertProductSettings(productSettings: WCProductSettingsModel)

    @Query(
        """
            SELECT * FROM ProductSettingsEntity
            WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun getProductSettings(siteId: LocalId): WCProductSettingsModel?
}
