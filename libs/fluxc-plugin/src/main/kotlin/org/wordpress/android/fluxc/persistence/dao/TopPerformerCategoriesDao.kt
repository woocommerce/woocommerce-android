package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.TopPerformerCategoryEntity

@Dao
interface TopPerformerCategoriesDao {
    @Query("SELECT * FROM TopPerformerCategories WHERE datePeriod = :datePeriod AND localSiteId = :localSiteId")
    fun observeTopPerformerCategories(
        localSiteId: LocalId,
        datePeriod: String
    ): Flow<List<TopPerformerCategoryEntity>>

    @Query("SELECT * FROM TopPerformerCategories WHERE datePeriod = :datePeriod AND localSiteId = :localSiteId")
    suspend fun getTopPerformerCategoriesFor(
        localSiteId: LocalId,
        datePeriod: String
    ): List<TopPerformerCategoryEntity>

    @Query("SELECT * FROM TopPerformerCategories WHERE localSiteId = :localSiteId")
    suspend fun getTopPerformerCategoriesForSite(
        localSiteId: LocalId
    ): List<TopPerformerCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TopPerformerCategoryEntity)

    @Query("DELETE FROM TopPerformerCategories WHERE datePeriod = :datePeriod AND localSiteId = :localSiteId")
    suspend fun deleteAllFor(localSiteId: LocalId, datePeriod: String)

    @Transaction
    suspend fun updateTopPerformerCategoriesFor(
        localSiteId: LocalId,
        datePeriod: String,
        topPerformerCategories: List<TopPerformerCategoryEntity>
    ) {
        deleteAllFor(localSiteId, datePeriod)
        topPerformerCategories.forEach { category ->
            insert(category)
        }
    }

    @Transaction
    suspend fun updateTopPerformerCategoriesForSite(
        localSiteId: LocalId,
        topPerformerCategories: List<TopPerformerCategoryEntity>
    ) {
        topPerformerCategories.forEach { category ->
            insert(category)
        }
    }
}
