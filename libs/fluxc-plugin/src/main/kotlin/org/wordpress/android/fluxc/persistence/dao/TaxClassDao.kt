package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel

@Dao
internal abstract class TaxClassDao {
    @Query(
        """
        SELECT * FROM TaxClassEntity
        WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun getTaxClasses(siteId: LocalId): List<WCTaxClassModel>

    @Transaction
    open suspend fun replaceAll(siteId: LocalId, classes: List<WCTaxClassModel>) {
        deleteAll(siteId)
        insertAll(classes)
    }

    @Query(
        """
    DELETE FROM TaxClassEntity
    WHERE localSiteId = :siteId
    """
    )
    protected abstract suspend fun deleteAll(siteId: LocalId): Int

    @Insert
    protected abstract suspend fun insertAll(classes: List<WCTaxClassModel>)
}
