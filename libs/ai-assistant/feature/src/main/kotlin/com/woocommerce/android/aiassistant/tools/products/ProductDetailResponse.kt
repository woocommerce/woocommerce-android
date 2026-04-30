package com.woocommerce.android.aiassistant.tools.products

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wordpress.android.fluxc.model.WCProductModel

@Serializable
internal data class ProductDetailResponse(
    val id: Long,
    val name: String,
    val status: String,
    val type: String,
    val sku: String,
    @SerialName("regular_price") val regularPrice: String,
    @SerialName("sale_price") val salePrice: String,
    @SerialName("on_sale") val onSale: Boolean,
    @SerialName("manage_stock") val manageStock: Boolean,
    @SerialName("stock_quantity") val stockQuantity: Double,
    @SerialName("stock_status") val stockStatus: String,
    @SerialName("date_created") val dateCreated: String,
)

internal fun WCProductModel.toProductDetailResponse() = ProductDetailResponse(
    id = remoteProductId,
    name = name,
    status = status,
    type = type,
    sku = sku,
    regularPrice = regularPrice,
    salePrice = salePrice,
    onSale = onSale,
    manageStock = manageStock,
    stockQuantity = stockQuantity,
    stockStatus = stockStatus,
    dateCreated = dateCreated,
)
