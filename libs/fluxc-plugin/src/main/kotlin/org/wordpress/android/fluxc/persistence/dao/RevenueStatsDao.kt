package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

@Dao
internal abstract class RevenueStatsDao {
    @Query(
        """
        SELECT * FROM RevenueStatsEntity
        WHERE localSiteId = :siteId
        AND interval = :granularity
        AND startDate = :startDate
        AND endDate = :endDate
        """
    )
    abstract suspend fun getBySiteIntervalAndDate(
        siteId: LocalId,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String
    ): WCRevenueStatsModel?

    @Query(
        """
        SELECT * FROM RevenueStatsEntity
        WHERE localSiteId = :siteId
        AND rangeId = :rangeId
        """
    )
    abstract suspend fun getBySiteAndRangeId(
        siteId: LocalId,
        rangeId: String
    ): WCRevenueStatsModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: WCRevenueStatsModel)
}
