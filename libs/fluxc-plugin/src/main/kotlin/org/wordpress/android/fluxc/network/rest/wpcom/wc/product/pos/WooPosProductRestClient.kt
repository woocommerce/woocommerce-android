package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pos.WooPosGenerateCatalogResponse
import org.wordpress.android.fluxc.model.pos.WooPosVariationApiResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.util.AppLog
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

class WooPosProductRestClient @Inject constructor(
    private val wooNetwork: WooNetwork,
) {
    companion object {
        private const val PRODUCT_FIELDS = "id,name,sku,global_unique_id,type,price,downloadable," +
            "images,attributes,parent_id,status,regular_price,sale_price,on_sale,description," +
            "short_description,manage_stock,stock_quantity,stock_status,backorders_allowed," +
            "backordered,categories,tags,date_modified"

        private const val VARIATIONS_FIELDS = "id,parent_id,description,sku,global_unique_id,status,price," +
            "regular_price,sale_price,date_modified,stock_quantity,stock_status,manage_stock," +
            "backordered,attributes,image,downloadable,name,type"

        private val SECONDS_PER_HOUR = 1.hours.inWholeSeconds

        private val API_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    }

    suspend fun fetchProducts(
        site: SiteModel,
        page: Int,
        pageSize: Int,
        posProductsOnly: Boolean,
        modifiedAfter: String? = null,
        includeStatus: List<CoreProductStatus>? = null,
    ): WooResult<Array<ProductApiResponse>> {
        val url = WOOCOMMERCE.products.pathV3
        val params = buildBaseParams(
            pageSize = pageSize,
            page = page,
            modifiedAfter = modifiedAfter?.let { adjustUtcToSiteLocalTime(it, site.timezone) },
            fields = PRODUCT_FIELDS,
            includeStatus = includeStatus,
            posProductsOnly = posProductsOnly
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<ProductApiResponse>::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                WooResult(response.data ?: emptyArray(), headers = response.headers)
            }

            is WPAPIResponse.Error -> {
                WooResult(response.error.toWooError())
            }
        }
    }

    suspend fun fetchVariations(
        site: SiteModel,
        page: Int,
        pageSize: Int,
        posProductsOnly: Boolean,
        modifiedAfter: String? = null,
    ): WooResult<Array<WooPosVariationApiResponse>> {
        val url = WOOCOMMERCE.variations.pathV3
        val params = buildBaseParams(
            pageSize = pageSize,
            page = page,
            fields = VARIATIONS_FIELDS,
            modifiedAfter = modifiedAfter?.let { adjustUtcToSiteLocalTime(it, site.timezone) },
            posProductsOnly = posProductsOnly
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<WooPosVariationApiResponse>::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                WooResult(response.data ?: emptyArray(), headers = response.headers)
            }

            is WPAPIResponse.Error -> {
                WooResult(response.error.toWooError())
            }
        }
    }

    suspend fun postGenerateCatalog(
        site: SiteModel,
        force: Boolean = false,
    ): WooResult<WooPosGenerateCatalogResponse> {
        val url = WOOCOMMERCE.catalog.create.pathPosV1
        val params = mutableMapOf(
            "_product_fields" to PRODUCT_FIELDS,
            "_variation_fields" to VARIATIONS_FIELDS
        )
        if (force) {
            params["force"] = "true"
        }

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = params,
            clazz = WooPosGenerateCatalogResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                WooResult(response.data)
            }

            is WPAPIResponse.Error -> {
                WooResult(response.error.toWooError())
            }
        }
    }

    private fun buildBaseParams(
        pageSize: Int,
        page: Int,
        fields: String,
        modifiedAfter: String?,
        includeStatus: List<CoreProductStatus>? = null,
        posProductsOnly: Boolean = false,
    ): MutableMap<String, String> {
        return mutableMapOf(
            "per_page" to pageSize.toString(),
            "page" to page.toString(),
            "_fields" to fields,
        ).also {
            modifiedAfter?.let { modified ->
                it["modified_after"] = modified
            }
            includeStatus?.let { statuses ->
                it["include_status"] = statusListToString(statuses)
            }
            if (posProductsOnly) {
                it["pos_products_only"] = "true"
            }
        }
    }

    private fun statusListToString(statuses: List<CoreProductStatus>): String {
        return statuses.joinToString(",") { it.value }
    }

    /**
     * The WooCommerce REST API compares `modified_after` against `date_modified`, which is stored
     * in the site's local timezone. Since our stored timestamps are in UTC (from server response
     * headers), we must convert to site-local time before sending. Without this, sites with
     * negative UTC offsets will miss recently modified products because the UTC value is always
     * ahead of their local `date_modified`.
     */
    internal fun adjustUtcToSiteLocalTime(utcDateString: String, siteGmtOffset: String?): String {
        val offsetHours = siteGmtOffset?.toDoubleOrNull() ?: return utcDateString
        if (offsetHours == 0.0) return utcDateString

        return try {
            val offsetSeconds = (offsetHours * SECONDS_PER_HOUR).toInt()
            val zoneOffset = ZoneOffset.ofTotalSeconds(offsetSeconds)
            val utcInstant = LocalDateTime.parse(utcDateString, API_DATE_FORMATTER)
                .toInstant(ZoneOffset.UTC)
            API_DATE_FORMATTER.withZone(zoneOffset).format(utcInstant)
        } catch (e: DateTimeException) {
            AppLog.e(AppLog.T.API, "Error adjusting UTC to site-local time- falling back to UTC.", e)
            utcDateString
        }
    }
}
