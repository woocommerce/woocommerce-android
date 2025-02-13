package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

data class BatchProductApiResponse(
    @SerializedName("update") val updatedProducts: List<ProductDto>? = null,
) : Response
