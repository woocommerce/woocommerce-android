package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel

@Dao
internal abstract class GlobalAttributesDao {
    @Query(
        """
        SELECT * FROM GlobalAttributeEntity
        WHERE siteId = :siteId
        """
    )
    abstract suspend fun getAttributesForSite(siteId: LocalId): List<WCGlobalAttributeModel>

    @Transaction
    open suspend fun replaceAllForSite(siteId: LocalId, attributes: List<WCGlobalAttributeModel>) {
        deleteAttributesForSite(siteId)
        upsertAttributes(attributes)
    }

    @Upsert
    protected abstract suspend fun upsertAttributes(attributes: List<WCGlobalAttributeModel>)

    @Query(
        """
        DELETE FROM GlobalAttributeEntity
        WHERE siteId = :siteId
        """
    )
    protected abstract suspend fun deleteAttributesForSite(siteId: LocalId)
}
