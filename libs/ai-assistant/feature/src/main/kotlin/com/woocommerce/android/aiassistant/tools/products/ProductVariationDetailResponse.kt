package com.woocommerce.android.aiassistant.tools.products

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wordpress.android.fluxc.model.WCProductVariationModel

@Serializable
internal data class ProductVariationDetailResponse(
    val id: Long,
    @SerialName("product_id") val productId: Long,
    val status: String,
    val sku: String,
    @SerialName("regular_price") val regularPrice: String,
    @SerialName("sale_price") val salePrice: String,
    @SerialName("on_sale") val onSale: Boolean,
    @SerialName("manage_stock") val manageStock: Boolean,
    @SerialName("stock_quantity") val stockQuantity: Double,
    @SerialName("stock_status") val stockStatus: String,
)

internal fun WCProductVariationModel.toProductVariationDetailResponse() =
    ProductVariationDetailResponse(
        id = remoteVariationId.value,
        productId = remoteProductId.value,
        status = status,
        sku = sku,
        regularPrice = regularPrice,
        salePrice = salePrice,
        onSale = onSale,
        manageStock = manageStock,
        stockQuantity = stockQuantity,
        stockStatus = stockStatus,
    )
