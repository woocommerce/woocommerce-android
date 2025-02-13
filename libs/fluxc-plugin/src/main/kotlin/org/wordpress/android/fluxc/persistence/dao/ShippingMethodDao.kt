package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.ShippingMethodEntity

@Dao
abstract class ShippingMethodDao {
    @Query("SELECT * FROM ShippingMethod WHERE localSiteId = :localSiteId")
    abstract fun observeShippingMethods(localSiteId: LocalId): Flow<List<ShippingMethodEntity>>

    @Query("SELECT * FROM ShippingMethod WHERE localSiteId = :localSiteId AND id = :id")
    abstract suspend fun getShippingMethodById(localSiteId: LocalId, id: String): ShippingMethodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertShippingMethods(shippingMethods: List<ShippingMethodEntity>)

    @Query("DELETE FROM ShippingMethod WHERE localSiteId = :localSiteId")
    abstract suspend fun deleteShippingMethodsForSite(localSiteId: LocalId)

    @Transaction
    open suspend fun updateShippingMethods(shippingMethods: List<ShippingMethodEntity>) {
        val localSiteId = shippingMethods.firstOrNull()?.localSiteId ?: return
        deleteShippingMethodsForSite(localSiteId)
        insertShippingMethods(shippingMethods)
    }
}
