package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCVisitorStatsSummary
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

@Entity(
    primaryKeys = ["localSiteId", "date", "granularity"]
)
data class VisitorSummaryStatsEntity(
    val localSiteId: LocalId,
    val date: String,
    val granularity: String,
    val views: Int,
    val visitors: Int
)

fun VisitorSummaryStatsEntity.toDomainModel() = WCVisitorStatsSummary(
    date = date,
    granularity = StatsGranularity.valueOf(granularity),
    views = views,
    visitors = visitors
)
