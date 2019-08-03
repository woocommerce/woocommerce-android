package com.woocommerce.android.model

import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

data class Product(
    val remoteId: Long,
    val name: String,
    val type: ProductType,
    val status: ProductStatus?,
    val stockStatus: ProductStockStatus,
    val backorderStatus: ProductBackorderStatus,
    val dateCreated: Date,
    val firstImageUrl: String?,
    val totalSales: Int,
    val reviewsAllowed: Boolean,
    val isVirtual: Boolean,
    val ratingCount: Int,
    val averageRating: Float,
    val permalink: String,
    val externalUrl: String,
    val price: BigDecimal?,
    val salePrice: BigDecimal?,
    val regularPrice: BigDecimal?,
    val taxClass: String,
    val manageStock: Boolean,
    val stockQuantity: Int,
    val sku: String,
    val length: Float,
    val width: Float,
    val height: Float,
    val weight: Float,
    val shippingClass: String,
    val isDownloadable: Boolean,
    val fileCount: Int,
    val downloadLimit: Int,
    val downloadExpiry: Int,
    val purchaseNote: String
)

fun WCProductModel.toAppModel(): Product {
    return Product(
        this.remoteProductId,
        this.name,
        ProductType.fromString(this.type),
        ProductStatus.fromString(this.status),
        ProductStockStatus.fromString(this.stockStatus),
        ProductBackorderStatus.fromString(this.backorders),
        DateTimeUtils.dateFromIso8601(this.dateCreated) ?: Date(),
        this.getFirstImageUrl(),
        this.totalSales,
        this.reviewsAllowed,
        this.virtual,
        this.ratingCount,
        this.averageRating.toFloatOrNull() ?: 0f,
        this.permalink,
        this.externalUrl,
        this.price.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        this.salePrice.toBigDecimalOrNull(),
        this.regularPrice.toBigDecimalOrNull(),
        this.taxClass,
        this.manageStock,
        this.stockQuantity,
        this.sku,
        this.length.toFloatOrNull() ?: 0f,
        this.width.toFloatOrNull() ?: 0f,
        this.height.toFloatOrNull() ?: 0f,
        this.weight.toFloatOrNull() ?: 0f,
        this.shippingClass,
        this.downloadable,
        this.getDownloadableFiles().size,
        this.downloadLimit,
        this.downloadExpiry,
        this.purchaseNote
    )
}
