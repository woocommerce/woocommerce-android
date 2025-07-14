package org.wordpress.android.fluxc.network.rest.wpcom.wc.reports

import com.google.gson.annotations.SerializedName

data class ReportsProductApiResponse(
    @SerializedName("product_id")
    val productId: Long? = null,
    @SerializedName("variation_id")
    val variationId: Long? = null,
    @SerializedName("items_sold")
    val itemsSold: Int? = null,
    @SerializedName("net_revenue")
    val netRevenue: Double? = null,
    @SerializedName("orders_count")
    val ordersCount: Long? = null,
    @SerializedName("extended_info")
    val product: ReportProductItem? = null
)

data class ReportProductItem(
    val name: String? = null,
    val price: Double? = null,
    @SerializedName("image")
    val imageHTML: String? = null
) {
    companion object {
        private val SRC_REGEX = Regex("src=\"(.*?)\"")
    }

    val imageUrl: String?
        get() = imageHTML
            ?.let { SRC_REGEX.find(it)?.value }
            ?.replace("src=", "")
            ?.replace("\"", "")
}

data class ProductStockItemApiResponse(
    @SerializedName("id")
    val productId: Long? = null,
    @SerializedName("parent_id")
    val parentId: Long? = null, // When the product is a variation, this is the parent product ID
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("stock_status")
    val stockStatus: String? = null,
    @SerializedName("stock_quantity")
    val stockQuantity: Int? = null,
    @SerializedName("low_stock_amount")
    val lowStockAmount: Int? = null
)
