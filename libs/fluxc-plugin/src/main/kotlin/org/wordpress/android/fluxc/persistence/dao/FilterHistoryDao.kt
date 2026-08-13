package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.FilterHistoryEntity

@Dao
interface FilterHistoryDao {
    /**
     * Inserts a filter history entry, or replaces an existing one with the same
     * (localSiteId, filterType, payload) so re-saving an identical selection bumps it to the top.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: FilterHistoryEntity): Long

    @Query(
        "SELECT * FROM FilterHistory WHERE localSiteId = :localSiteId AND filterType = :filterType " +
            "ORDER BY dateModified DESC"
    )
    fun observeForSite(localSiteId: LocalId, filterType: String): Flow<List<FilterHistoryEntity>>

    @Query("DELETE FROM FilterHistory WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM FilterHistory WHERE localSiteId = :localSiteId AND filterType = :filterType")
    suspend fun clear(localSiteId: LocalId, filterType: String): Int
}
