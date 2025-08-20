package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import javax.inject.Inject

class PosProductRestClient @Inject constructor(
    private val wooNetwork: WooNetwork,
) {
    companion object {
        private const val FIELDS = "localSiteId,id,name,sku,global_unique_id,type,price,downloadable," +
            "images,attributes,parent_id,status,regular_price,sale_price,on_sale,description," +
            "short_description,manage_stock,stock_quantity,stock_status,backorders_allowed," +
            "backordered,categories,tags,date_modified"
    }

    @Suppress("LongMethod")
    suspend fun fetchProducts(
        site: SiteModel,
        modifiedAfter: String? = null,
        offset: Int = 0,
    ): Result<Array<ProductApiResponse>> {
        val url = WOOCOMMERCE.products.pathV3
        val params = buildBaseParams(
            pageSize = 100,
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
                Result.success(response.data!!)
            }

            is WPAPIResponse.Error -> {
                Result.failure(IllegalStateException())
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

    // TODO copy pasted - likely should be re-used.
    private fun wpAPINetworkErrorToProductError(wpAPINetworkError: WPAPINetworkError): ProductError {
        val productErrorType = when {
            wpAPINetworkError.errorCode == "woocommerce_rest_product_invalid_id" ->
                ProductErrorType.INVALID_PRODUCT_ID

            wpAPINetworkError.errorCode == "rest_invalid_param" -> ProductErrorType.INVALID_PARAM
            wpAPINetworkError.errorCode == "woocommerce_rest_review_invalid_id" ->
                ProductErrorType.INVALID_REVIEW_ID

            wpAPINetworkError.errorCode == "woocommerce_product_invalid_image_id" ->
                ProductErrorType.INVALID_IMAGE_ID

            wpAPINetworkError.errorCode == "product_invalid_sku" -> ProductErrorType.DUPLICATE_SKU
            wpAPINetworkError.errorCode == "term_exists" -> ProductErrorType.TERM_EXISTS
            wpAPINetworkError.errorCode == "woocommerce_variation_invalid_image_id" ->
                ProductErrorType.INVALID_VARIATION_IMAGE_ID

            wpAPINetworkError.errorCode == "woocommerce_rest_invalid_min_quantity" ||
                wpAPINetworkError.errorCode == "woocommerce_rest_invalid_max_quantity" ||
                wpAPINetworkError.errorCode == "woocommerce_rest_invalid_variation_min_quantity" ||
                wpAPINetworkError.errorCode == "woocommerce_rest_invalid_variation_max_quantity" ->
                ProductErrorType.INVALID_MIN_MAX_QUANTITY

            wpAPINetworkError.type == PARSE_ERROR -> ProductErrorType.PARSE_ERROR
            else -> ProductErrorType.fromString(wpAPINetworkError.errorCode.orEmpty())
        }
        return ProductError(productErrorType, wpAPINetworkError.combinedErrorMessage)
    }
}
