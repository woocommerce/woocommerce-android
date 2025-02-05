package org.wordpress.android.fluxc.model.leaderboards

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardProductItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsProductApiResponse
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.ProductSqlUtils.geProductExistsByRemoteId
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WCProductLeaderboardsMapper @Inject constructor() {
    suspend fun mapTopPerformerProductsEntity(
        response: LeaderboardsApiResponse,
        site: SiteModel,
        productStore: WCProductStore,
        datePeriod: String
    ): List<TopPerformerProductEntity> = response.products
        ?.takeIf { it.isNotEmpty() }
        ?.mapNotNull { it.productId }
        ?.asProductList(site, productStore)
        ?.mapNotNull { product ->
            response.products
                ?.find { it.productId == product.remoteProductId }
                ?.let { product.toTopPerformerProductEntity(it, site, datePeriod) }
        }.orEmpty()

    /**
     * This method fetch and request all Products from the IDs described by the
     * List<Long>, but it only requests to the site products who doesn't exist
     * inside the database to avoid unnecessary data traffic.
     *
     * Please note that we must request first the local products and second the
     * remote products, if we invert this order the remotely fetched products will
     * be inserted inside the database by the [WCProductStore] natural behavior, and
     * when we fetch the local products after that we will end up unnecessarily duplicating
     * data, since they will be both fetched remotely and locally.
     */
    private suspend fun List<Long>.asProductList(
        site: SiteModel,
        productStore: WCProductStore
    ): List<WCProductModel> {
        val locallyFetchedProducts = this
                .filter { geProductExistsByRemoteId(site, it) }
                .mapNotNull { ProductSqlUtils.getProductByRemoteId(site, it) }

        val remotelyFetchedProducts = this
                .filter { geProductExistsByRemoteId(site, it).not() }
                .takeIf { it.isNotEmpty() }
                ?.let { productStore.fetchProductListSynced(site, it) }
                .orEmpty()

        return mutableListOf<WCProductModel>().apply {
            addAll(remotelyFetchedProducts)
            addAll(locallyFetchedProducts)
        }.toList()
    }

    private fun WCProductModel.toTopPerformerProductEntity(
        productItem: LeaderboardProductItem,
        site: SiteModel,
        datePeriod: String
    ) = TopPerformerProductEntity(
        localSiteId = site.localId(),
        datePeriod = datePeriod,
        productId = remoteId,
        name = name,
        imageUrl = getFirstImageUrl(),
        quantity = productItem.quantity?.toIntOrNull() ?: 0,
        currency = productItem.currency.toString(),
        total = productItem.total?.toDoubleOrNull() ?: 0.0,
        millisSinceLastUpdated = System.currentTimeMillis()
    )

    fun mapTopPerformerProductsEntity(
        response: ReportsProductApiResponse,
        site: SiteModel,
        datePeriod: String,
        currencyCode: String
    ): TopPerformerProductEntity {
        val imageURL = response.product?.imageUrl
        return TopPerformerProductEntity(
            localSiteId = site.localId(),
            datePeriod = datePeriod,
            productId = RemoteId(response.productId ?: 0L),
            name = response.product?.name ?: "",
            imageUrl = imageURL,
            quantity = response.itemsSold ?: 0,
            currency = currencyCode,
            total = response.netRevenue ?: 0.0,
            millisSinceLastUpdated = System.currentTimeMillis()
        )
    }
}
