package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

@Dao
internal abstract class NewVisitorStatsDao {
    @Query(
        """
        SELECT * FROM NewVisitorStatsEntity
        WHERE localSiteId = :siteId
        AND granularity = :granularity
        AND date = :date
        AND quantity = :quantity
        """
    )
    abstract suspend fun getStat(
        siteId: LocalId,
        granularity: StatsGranularity,
        quantity: String?,
        date: String?,
    ): WCNewVisitorStatsModel?

    @Transaction
    open suspend fun replaceStat(entity: WCNewVisitorStatsModel) {
        deleteStatForSite(entity.localSiteId)
        insertStat(entity)
    }

    @Query(
        """
        DELETE FROM NewVisitorStatsEntity
        WHERE localSiteId = :siteId
        """
    )
    protected abstract suspend fun deleteStatForSite(siteId: LocalId): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertStat(entity: WCNewVisitorStatsModel)
}
