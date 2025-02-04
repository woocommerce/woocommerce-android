package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

/**
 * Representation of Batch Update Product Variations API response.
 */
data class BatchProductVariationsApiResponse(
    @SerializedName("create") val createdVariations: List<ProductVariationApiResponse>? = null,
    @SerializedName("update") val updatedVariations: List<ProductVariationApiResponse>? = null,
    @SerializedName("delete") val deletedVariations: List<Long>? = null
) : Response
