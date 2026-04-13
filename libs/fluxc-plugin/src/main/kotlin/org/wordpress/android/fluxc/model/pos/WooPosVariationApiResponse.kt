package org.wordpress.android.fluxc.model.pos

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Response model for WooCommerce Analytics variations endpoint (/wc-analytics/variations).
 */
data class WooPosVariationApiResponse(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("parent_id")
    val productId: Long = 0L,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("sku")
    val sku: String? = null,

    @SerializedName("global_unique_id")
    val globalUniqueId: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("price")
    val price: String? = null,

    @SerializedName("regular_price")
    val regularPrice: String? = null,

    @SerializedName("sale_price")
    val salePrice: String? = null,

    @SerializedName("date_modified")
    val dateModified: String? = null,

    @SerializedName("stock_quantity")
    val stockQuantity: Double? = null,

    @SerializedName("stock_status")
    val stockStatus: String? = null,

    @SerializedName("manage_stock")
    val manageStock: Boolean = false,

    @SerializedName("backordered")
    val backordered: Boolean = false,

    @SerializedName("attributes")
    val attributesJson: JsonElement? = null,

    @SerializedName("image")
    val image: VariationImage? = null,

    @SerializedName("downloadable")
    val downloadable: Boolean = false,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("type")
    val type: String? = null
) {
    data class VariationImage(
        @SerializedName("id")
        val id: Long = 0L,

        @SerializedName("src")
        val src: String? = null,

        @SerializedName("name")
        val name: String? = null,

        @SerializedName("alt")
        val alt: String? = null
    )
}
