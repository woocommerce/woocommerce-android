package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pos.WooPosCatalogStatusResponse
import org.wordpress.android.fluxc.model.pos.WooPosGenerateCatalogResponse
import org.wordpress.android.fluxc.model.pos.WooPosVariationApiResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject

class WooPosProductRestClient @Inject constructor(
    private val wooNetwork: WooNetwork,
) {
    companion object Companion {
        private const val PRODUCT_FIELDS = "localSiteId,id,name,sku,global_unique_id,type,price,downloadable," +
            "images,attributes,parent_id,status,regular_price,sale_price,on_sale,description," +
            "short_description,manage_stock,stock_quantity,stock_status,backorders_allowed," +
            "backordered,categories,tags,date_modified"

        private const val VARIATIONS_FIELDS = "id,parent_id,description,sku,global_unique_id,status,price," +
            "regular_price,sale_price,date_modified,stock_quantity,stock_status,manage_stock," +
            "backordered,attributes,image,downloadable,name"
    }

    suspend fun fetchProducts(
        site: SiteModel,
        modifiedAfter: String? = null,
        offset: Int,
        pageSize: Int,
    ): WooResult<Array<ProductApiResponse>> {
        val url = WOOCOMMERCE.products.pathV3
        val params = buildBaseParams(
            pageSize = pageSize,
            offset = offset,
            modifiedAfter = modifiedAfter,
            fields = PRODUCT_FIELDS
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<ProductApiResponse>::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                WooResult(response.data ?: emptyArray())
            }

            is WPAPIResponse.Error -> {
                WooResult(response.error.toWooError())
            }
        }
    }

    suspend fun fetchVariations(
        site: SiteModel,
        modifiedAfter: String? = null,
        page: Int,
        pageSize: Int,
    ): WooResult<Array<WooPosVariationApiResponse>> {
        val url = "/wc-analytics/variations"
        val params = mutableMapOf(
            "per_page" to pageSize.toString(),
            "paged" to page.toString(),
            "_fields" to VARIATIONS_FIELDS
        ).also {
            if (modifiedAfter.isNullOrBlank().not()) {
                it["modified_after"] = modifiedAfter
            }
        }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<WooPosVariationApiResponse>::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                WooResult(response.data ?: emptyArray())
            }

            is WPAPIResponse.Error -> {
                WooResult(response.error.toWooError())
            }
        }
    }

    suspend fun postGenerateCatalog(
        site: SiteModel,
    ): WooResult<WooPosGenerateCatalogResponse> {
        val url = WOOCOMMERCE.catalog.pathV3
        val params = mutableMapOf(
            "_fields" to PRODUCT_FIELDS
        )

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

    suspend fun getCatalogStatus(
        site: SiteModel,
        jobId: String,
    ): WooResult<WooPosCatalogStatusResponse> {
        val url = WOOCOMMERCE.catalog.status.id(jobId).pathV3
        val params = mutableMapOf(
            "_fields" to PRODUCT_FIELDS
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = WooPosCatalogStatusResponse::class.java
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
        offset: Int,
        fields: String,
        modifiedAfter: String?,
    ): MutableMap<String, String> {
        return mutableMapOf(
            "per_page" to pageSize.toString(),
            "offset" to offset.toString(),
            "_fields" to fields,

            ).also {
            modifiedAfter?.let { modified ->
                it["modified_after"] = modified
            }
        }
    }
}
