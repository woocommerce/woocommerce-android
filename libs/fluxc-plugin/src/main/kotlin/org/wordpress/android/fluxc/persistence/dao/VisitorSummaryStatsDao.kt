package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.VisitorSummaryStatsEntity

@Dao
interface VisitorSummaryStatsDao {
    @Query(
        """
        SELECT * FROM VisitorSummaryStatsEntity 
        WHERE localSiteId = :siteId 
        AND granularity = :granularity 
        AND date = :date
        """
    )
    suspend fun getVisitorSummaryStats(siteId: LocalId, granularity: String, date: String): VisitorSummaryStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VisitorSummaryStatsEntity)
}
