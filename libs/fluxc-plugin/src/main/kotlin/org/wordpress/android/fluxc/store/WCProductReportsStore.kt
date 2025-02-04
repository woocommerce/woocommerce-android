package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ProductStockItemApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCProductReportsStore @Inject constructor(
    private val reportsRestClient: ReportsRestClient,
    private val coroutineEngine: CoroutineEngine,
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 3
        const val DEFAULT_PAGE = 1
    }

    suspend fun fetchProductStockReport(
        site: SiteModel,
        stockStatus: CoreProductStockStatus,
        page: Int = DEFAULT_PAGE,
        quantity: Int = DEFAULT_PAGE_SIZE
    ): WooResult<ProductStockItems> =
        coroutineEngine.withDefaultContext(API, this, "fetchProductStockReport") {
            reportsRestClient.fetchProductStockReport(
                site = site,
                stockStatus = stockStatus.value,
                page = page,
                quantity = quantity
            )
        }.asWooResult()

    suspend fun fetchProductSalesReport(
        site: SiteModel,
        startDate: String,
        endDate: String,
        productIds: List<Long>
    ): WooResult<Array<ReportsProductApiResponse>> =
        coroutineEngine.withDefaultContext(API, this, "fetchProductSalesReport") {
            reportsRestClient.fetchProductSalesReport(
                site,
                startDate,
                endDate,
                productIds
            )
        }.asWooResult()

    suspend fun fetchProductVariationsSalesReport(
        site: SiteModel,
        startDate: String,
        endDate: String,
        productVariationIds: List<Long>
    ): WooResult<Array<ReportsProductApiResponse>> =
        coroutineEngine.withDefaultContext(API, this, "fetchProductVariationsSalesReport") {
            reportsRestClient.fetchProductVariationsSalesReport(
                site,
                startDate,
                endDate,
                productVariationIds
            )
        }.asWooResult()
}

typealias ProductStockItems = Array<ProductStockItemApiResponse>
