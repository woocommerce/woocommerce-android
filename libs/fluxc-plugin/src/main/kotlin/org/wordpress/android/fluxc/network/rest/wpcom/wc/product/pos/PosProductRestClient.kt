package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pos.PosVariationApiResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject

class PosProductRestClient @Inject constructor(
    private val wooNetwork: WooNetwork,
) {
    companion object {
        private const val FIELDS = "localSiteId,id,name,sku,global_unique_id,type,price,downloadable," +
            "images,attributes,parent_id,status,regular_price,sale_price,on_sale,description," +
            "short_description,manage_stock,stock_quantity,stock_status,backorders_allowed," +
            "backordered,categories,tags,date_modified"

        private const val VARIATIONS_FIELDS = "id,parent_id,description,sku,global_unique_id,status,price," +
            "regular_price,sale_price,date_modified,stock_quantity,stock_status,manage_stock," +
            "backordered,attributes,image,downloadable"
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
            fields = FIELDS
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
    ): WooResult<Array<PosVariationApiResponse>> {
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
            clazz = Array<PosVariationApiResponse>::class.java
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
