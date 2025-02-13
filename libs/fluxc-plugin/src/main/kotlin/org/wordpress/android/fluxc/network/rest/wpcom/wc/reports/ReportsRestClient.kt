package org.wordpress.android.fluxc.network.rest.wpcom.wc.reports

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class ReportsRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchTopPerformerProducts(
        site: SiteModel,
        startDate: String,
        endDate: String,
        quantity: Int = 5
    ): WooPayload<Array<ReportsProductApiResponse>> {
        fun createParameters(
            startDate: String,
            endDate: String,
            quantity: Int
        ) = mapOf(
            "before" to endDate,
            "after" to startDate,
            "per_page" to quantity.toString(),
            "extended_info" to "true",
            "orderby" to "items_sold",
            "order" to "desc"
        ).filter { it.value.isNotEmpty() }

        val url = WOOCOMMERCE.reports.products.pathV4Analytics
        val parameters = createParameters(startDate, endDate, quantity)

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<ReportsProductApiResponse>::class.java,
            params = parameters
        )

        return response.toWooPayload()
    }

    suspend fun fetchProductStockReport(
        site: SiteModel,
        stockStatus: String,
        page: Int,
        quantity: Int
    ): WooPayload<Array<ProductStockItemApiResponse>> {
        val url = WOOCOMMERCE.reports.stock.pathV4Analytics
        val parameters = mapOf(
            "page" to page.toString(),
            "per_page" to quantity.toString(),
            "order" to "asc",
            "type" to stockStatus
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<ProductStockItemApiResponse>::class.java,
            params = parameters
        )

        return response.toWooPayload()
    }

    suspend fun fetchProductSalesReport(
        site: SiteModel,
        startDate: String,
        endDate: String,
        productIds: List<Long>
    ): WooPayload<Array<ReportsProductApiResponse>> {
        val url = WOOCOMMERCE.reports.products.pathV4Analytics
        val response = fetchSales(
            endDate = endDate,
            startDate = startDate,
            productIds = productIds,
            site = site,
            url = url,
            quantity = productIds.size
        )
        return response.toWooPayload()
    }

    suspend fun fetchProductVariationsSalesReport(
        site: SiteModel,
        startDate: String,
        endDate: String,
        variationIds: List<Long>
    ): WooPayload<Array<ReportsProductApiResponse>> {
        val url = WOOCOMMERCE.reports.variations.pathV4Analytics
        val response = fetchSales(
            endDate = endDate,
            startDate = startDate,
            variationIds = variationIds,
            site = site,
            url = url,
            quantity = variationIds.size
        )
        return response.toWooPayload()
    }

    private suspend fun fetchSales(
        endDate: String,
        startDate: String,
        productIds: List<Long> = emptyList(),
        variationIds: List<Long> = emptyList(),
        site: SiteModel,
        url: String,
        quantity: Int
    ): WPAPIResponse<Array<ReportsProductApiResponse>> {
        val parameters = mapOf(
            "before" to endDate,
            "after" to startDate,
            "products" to productIds.joinToString(","),
            "variations" to variationIds.joinToString(","),
            "extended_info" to "true",
            "orderby" to "items_sold",
            "order" to "desc",
            "page" to "1",
            "per_page" to quantity.toString()
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<ReportsProductApiResponse>::class.java,
            params = parameters
        )
        return response
    }
}
