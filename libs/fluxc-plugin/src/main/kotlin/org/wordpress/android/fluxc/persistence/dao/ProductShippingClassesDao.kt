package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

@Dao
internal abstract class ProductShippingClassesDao {

    @Query(
        """
            SELECT * FROM ProductShippingClassEntity
            WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun getProductShippingClasses(siteId: LocalId): List<WCProductShippingClassModel>

    @Query(
        """
            SELECT * FROM ProductShippingClassEntity
            WHERE localSiteId = :siteId
            AND remoteShippingClassId = :remoteShippingClassId
        """
    )
    abstract suspend fun getProductShippingClass(
        siteId: LocalId,
        remoteShippingClassId: RemoteId,
    ): WCProductShippingClassModel?


    @Query(
        """
            DELETE FROM ProductShippingClassEntity
            WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun deleteProductShippingClasses(
        siteId: LocalId,
    )

    @Upsert
    abstract suspend fun upsertProductShippingClasses(
        shippingClasses: List<WCProductShippingClassModel>,
    )

    @Upsert
    abstract suspend fun upsertProductShippingClass(
        shippingClass: WCProductShippingClassModel,
    )
}
