package org.wordpress.android.fluxc.model.pos

import com.google.gson.annotations.SerializedName

/**
 * Response model for WooCommerce Analytics variations endpoint (/wc-analytics/variations).
 */
data class PosVariationApiResponse(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("parent_id")
    val productId: Long = 0L,

    @SerializedName("description")
    val description: String = "",

    @SerializedName("sku")
    val sku: String = "",

    @SerializedName("global_unique_id")
    val globalUniqueId: String = "",

    @SerializedName("status")
    val status: String = "",

    @SerializedName("price")
    val price: String = "",

    @SerializedName("regular_price")
    val regularPrice: String = "",

    @SerializedName("sale_price")
    val salePrice: String = "",

    @SerializedName("date_modified")
    val dateModified: String = "",

    @SerializedName("stock_quantity")
    val stockQuantity: Double? = null,

    @SerializedName("stock_status")
    val stockStatus: String = "",

    @SerializedName("manage_stock")
    val manageStock: Boolean = false,

    @SerializedName("backordered")
    val backordered: Boolean = false,

    @SerializedName("attributes")
    val attributes: List<VariationAttribute> = emptyList(),

    @SerializedName("image")
    val image: VariationImage? = null,

    @SerializedName("downloadable")
    val downloadable: Boolean = false
) {
    data class VariationAttribute(
        @SerializedName("id")
        val id: Long = 0L,

        @SerializedName("name")
        val name: String = "",

        @SerializedName("option")
        val option: String = ""
    )

    data class VariationImage(
        @SerializedName("id")
        val id: Long = 0L,

        @SerializedName("src")
        val src: String = "",

        @SerializedName("name")
        val name: String = "",

        @SerializedName("alt")
        val alt: String = ""
    )
}
