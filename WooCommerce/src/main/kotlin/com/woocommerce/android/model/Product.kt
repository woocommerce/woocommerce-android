package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.formatDateToISO8601Format
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class Product(
    val remoteId: Long,
    var name: String,
    var description: String,
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
    val purchaseNote: String,
    val numVariations: Int,
    val images: List<Image>,
    val attributes: List<Attribute>,
    val dateOnSaleTo: Date?,
    val dateOnSaleFrom: Date?
) : Parcelable {
    @Parcelize
    data class Image(
        val id: Long,
        val name: String,
        val source: String,
        val dateCreated: Date
    ) : Parcelable

    @Parcelize
    data class Attribute(
        val id: Long,
        val name: String,
        val options: List<String>,
        val isVisible: Boolean
    ) : Parcelable

    fun isSameProduct(product: Product): Boolean {
        return remoteId == product.remoteId &&
                stockQuantity == product.stockQuantity &&
                stockStatus == product.stockStatus &&
                status == product.status &&
                manageStock == product.manageStock &&
                type == product.type &&
                numVariations == product.numVariations &&
                name == product.name &&
                description == product.description &&
                images == product.images
    }

    /**
     * Method merges the updated product fields edited by the user with the locally cached
     * [Product] model and returns the updated [Product] model.
     *
     * [newProduct] includes the updated product fields edited by the user.
     * if [newProduct] is not null, a copy of the stored [Product] model is created
     * and product fields edited by the user and added to this model before returning it
     *
     */
    fun mergeProduct(newProduct: Product?): Product {
        return newProduct?.let { updatedProduct ->
            this.copy().apply {
                if (updatedProduct.description != this.description) {
                    description = updatedProduct.description
                }
                if (updatedProduct.name != this.name) {
                    name = updatedProduct.name
                }
            }
        } ?: this.copy()
    }
}

fun Product.toDataModel(storedProductModel: WCProductModel?): WCProductModel {
    return (storedProductModel ?: WCProductModel()).also {
        it.remoteProductId = remoteId
        it.description = description
        it.name = name
    }
}

fun WCProductModel.toAppModel(): Product {
    return Product(
        this.remoteProductId,
        this.name,
        this.description,
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
        this.price.toBigDecimalOrNull()?.roundError(),
        this.salePrice.toBigDecimalOrNull()?.roundError(),
        this.regularPrice.toBigDecimalOrNull()?.roundError(),
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
        this.purchaseNote,
        this.getNumVariations(),
        this.getImages().map {
            Product.Image(
                    it.id,
                    it.name,
                    it.src,
                    DateTimeUtils.dateFromIso8601(this.dateCreated) ?: Date()
            )
        },
        this.getAttributes().map {
            Product.Attribute(
                    it.id,
                    it.name,
                    it.options,
                    it.visible
            )
        },
        this.dateOnSaleTo.formatDateToISO8601Format(),
        this.dateOnSaleFrom.formatDateToISO8601Format()
    )
}

/**
 * Returns the product as a [ProductReviewProduct] for use with the product reviews feature.
 */
fun WCProductModel.toProductReviewProductModel() =
        ProductReviewProduct(this.remoteProductId, this.name, this.permalink)
