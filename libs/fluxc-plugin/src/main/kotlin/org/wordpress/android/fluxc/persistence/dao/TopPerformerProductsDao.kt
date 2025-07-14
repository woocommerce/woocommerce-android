package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity

@Dao
interface TopPerformerProductsDao {
    @Query("SELECT * FROM TopPerformerProducts WHERE datePeriod = :datePeriod AND localSiteId = :localSiteId")
    fun observeTopPerformerProducts(
        localSiteId: LocalId,
        datePeriod: String
    ): Flow<List<TopPerformerProductEntity>>

    @Query("SELECT * FROM TopPerformerProducts WHERE datePeriod = :datePeriod AND localSiteId = :localSiteId")
    suspend fun getTopPerformerProductsFor(
        localSiteId: LocalId,
        datePeriod: String
    ): List<TopPerformerProductEntity>

    @Query("SELECT * FROM TopPerformerProducts WHERE localSiteId = :localSiteId")
    suspend fun getTopPerformerProductsForSite(
        localSiteId: LocalId
    ): List<TopPerformerProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TopPerformerProductEntity)

    @Query("DELETE FROM TopPerformerProducts WHERE datePeriod = :datePeriod AND localSiteId = :localSiteId")
    suspend fun deleteAllFor(localSiteId: LocalId, datePeriod: String)

    @Transaction
    suspend fun updateTopPerformerProductsFor(
        localSiteId: LocalId,
        datePeriod: String,
        topPerformerProducts: List<TopPerformerProductEntity>
    ) {
        deleteAllFor(localSiteId, datePeriod)
        topPerformerProducts.forEach { topPerformerProduct ->
            insert(topPerformerProduct)
        }
    }

    @Transaction
    suspend fun updateTopPerformerProductsForSite(
        localSiteId: LocalId,
        topPerformerProducts: List<TopPerformerProductEntity>
    ) {
        topPerformerProducts.forEach { topPerformerProduct ->
            insert(topPerformerProduct)
        }
    }
}
