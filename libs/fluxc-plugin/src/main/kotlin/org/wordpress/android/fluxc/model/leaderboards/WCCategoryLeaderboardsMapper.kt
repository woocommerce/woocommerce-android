package org.wordpress.android.fluxc.model.leaderboards

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.persistence.entity.TopPerformerCategoryEntity
import javax.inject.Inject

class WCCategoryLeaderboardsMapper @Inject constructor() {
    fun mapTopPerformerCategoriesEntity(
        response: LeaderboardsApiResponse,
        site: SiteModel,
        datePeriod: String
    ): List<TopPerformerCategoryEntity> = response.categories
        ?.mapNotNull { categoryItem ->
            val categoryId = categoryItem.categoryId ?: return@mapNotNull null
            TopPerformerCategoryEntity(
                localSiteId = site.localId(),
                datePeriod = datePeriod,
                categoryId = RemoteId(categoryId),
                name = categoryItem.name ?: "",
                quantity = categoryItem.quantity?.toIntOrNull() ?: 0,
                currency = categoryItem.currency.toString(),
                total = categoryItem.total?.toDoubleOrNull() ?: 0.0,
                millisSinceLastUpdated = System.currentTimeMillis()
            )
        }.orEmpty()
}
