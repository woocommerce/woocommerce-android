package com.woocommerce.android.ui.products.filter

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import javax.inject.Inject

/**
 * Encodes/decodes a [ProductFilterResult] to and from the opaque `payload` string persisted in the
 * filter history table.
 *
 * The payload is a JSON serialization of the selected slugs; Gson omits null fields, so logically
 * identical selections produce identical JSON and dedup reliably. The category name is only persisted
 * when a category id is actually selected, so clearing the category to "Any" can't leak a stale name
 * into the payload (which would break dedup). Decoding tolerates missing/unknown fields (all data-class
 * fields default to null).
 */
class ProductFilterHistoryMapper @Inject constructor(
    private val gson: Gson
) {
    fun toPayload(filter: ProductFilterResult): String = gson.toJson(filter.toData())

    fun fromPayload(payload: String): ProductFilterResult? =
        runCatching { gson.fromJson(payload, ProductFilterHistoryData::class.java) }.getOrNull()?.toResult()

    private fun ProductFilterResult.toData() = ProductFilterHistoryData(
        stockStatus = stockStatus,
        productStatus = productStatus,
        productType = productType,
        productCategory = productCategory,
        productCategoryName = productCategoryName?.takeIf { productCategory != null }
    )

    private fun ProductFilterHistoryData.toResult() = ProductFilterResult(
        stockStatus = stockStatus,
        productType = productType,
        productStatus = productStatus,
        productCategory = productCategory,
        productCategoryName = productCategoryName
    )

    data class ProductFilterHistoryData(
        @SerializedName("stock_status") val stockStatus: String? = null,
        @SerializedName("product_status") val productStatus: String? = null,
        @SerializedName("product_type") val productType: String? = null,
        @SerializedName("product_category") val productCategory: String? = null,
        @SerializedName("product_category_name") val productCategoryName: String? = null
    )
}
