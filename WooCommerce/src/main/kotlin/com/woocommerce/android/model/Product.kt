package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.woocommerce.android.R
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.formatDateToISO8601Format
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.extensions.isEquivalentTo
import com.woocommerce.android.extensions.isNotSet
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTaxStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class Product(
    val remoteId: Long,
    val name: String,
    val description: String,
    val shortDescription: String,
    val slug: String,
    val type: ProductType,
    val status: ProductStatus?,
    val catalogVisibility: ProductCatalogVisibility?,
    val isFeatured: Boolean,
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
    val buttonText: String,
    val salePrice: BigDecimal?,
    val regularPrice: BigDecimal?,
    val taxClass: String,
    val isStockManaged: Boolean,
    val stockQuantity: Int,
    val sku: String,
    val shippingClass: String,
    val shippingClassId: Long,
    val isDownloadable: Boolean,
    val fileCount: Int,
    val downloadLimit: Int,
    val downloadExpiry: Int,
    val purchaseNote: String,
    val numVariations: Int,
    val images: List<Image>,
    val attributes: List<Attribute>,
    val saleEndDateGmt: Date?,
    val saleStartDateGmt: Date?,
    val isSoldIndividually: Boolean,
    val taxStatus: ProductTaxStatus,
    val isSaleScheduled: Boolean,
    val menuOrder: Int,
    val categories: List<ProductCategory>,
    val tags: List<ProductTag>,
    val groupedProductIds: List<Long>,
    override val length: Float,
    override val width: Float,
    override val height: Float,
    override val weight: Float
) : Parcelable, IProduct {
    companion object {
        const val TAX_CLASS_DEFAULT = "standard"
    }

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
            isStockManaged == product.isStockManaged &&
            backorderStatus == product.backorderStatus &&
            isSoldIndividually == product.isSoldIndividually &&
            reviewsAllowed == product.reviewsAllowed &&
            sku == product.sku &&
            slug == product.slug &&
            type == product.type &&
            numVariations == product.numVariations &&
            name.fastStripHtml() == product.name.fastStripHtml() &&
            description == product.description &&
            shortDescription == product.shortDescription &&
            taxClass == product.taxClass &&
            taxStatus == product.taxStatus &&
            isSaleScheduled == product.isSaleScheduled &&
            saleEndDateGmt == product.saleEndDateGmt &&
            saleStartDateGmt == product.saleStartDateGmt &&
            regularPrice isEquivalentTo product.regularPrice &&
            salePrice isEquivalentTo product.salePrice &&
            weight == product.weight &&
            length == product.length &&
            height == product.height &&
            width == product.width &&
            isVirtual == product.isVirtual &&
            shippingClass == product.shippingClass &&
            shippingClassId == product.shippingClassId &&
            catalogVisibility == product.catalogVisibility &&
            isFeatured == product.isFeatured &&
            purchaseNote == product.purchaseNote &&
            externalUrl == product.externalUrl &&
            buttonText == product.buttonText &&
            menuOrder == product.menuOrder &&
            isSameImages(product.images) &&
            isSameCategories(product.categories) &&
            isSameTags(product.tags) &&
            groupedProductIds == product.groupedProductIds
    }

    val hasCategories get() = categories.isNotEmpty()
    val hasTags get() = tags.isNotEmpty()
    val hasShortDescription get() = shortDescription.isNotEmpty()
    val hasShipping: Boolean
        get() {
            return weight > 0 ||
                length > 0 || width > 0 || height > 0 ||
                shippingClass.isNotEmpty()
        }

    /**
     * Verifies if there are any changes made to the inventory fields
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasInventoryChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            sku != it.sku ||
                isStockManaged != it.isStockManaged ||
                stockStatus != it.stockStatus ||
                stockQuantity != it.stockQuantity ||
                backorderStatus != it.backorderStatus ||
                isSoldIndividually != it.isSoldIndividually
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the shipping fields
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasShippingChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            weight != it.weight ||
                length != it.length ||
                width != it.width ||
                height != it.height ||
                shippingClass != it.shippingClass
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the product images
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasImageChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            !isSameImages(it.images)
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the external link settings
     */
    fun hasExternalLinkChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            externalUrl != it.externalUrl ||
                buttonText != it.buttonText
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the product settings
     */
    fun hasSettingsChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            status != it.status ||
                catalogVisibility != it.catalogVisibility ||
                isFeatured != it.isFeatured ||
                slug != it.slug ||
                reviewsAllowed != it.reviewsAllowed ||
                purchaseNote != it.purchaseNote ||
                menuOrder != it.menuOrder ||
                isVirtual != it.isVirtual
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the product categories
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasCategoryChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            !isSameCategories(it.categories)
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the product tags
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasTagChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            !isSameTags(it.tags)
        } ?: false
    }

    /**
     * Compares this product's images with the passed list, returns true only if both lists contain
     * the same images in the same order
     */
    private fun isSameImages(updatedImages: List<Image>): Boolean {
        if (this.images.size != updatedImages.size) {
            return false
        }
        for (i in images.indices) {
            if (images[i].id != updatedImages[i].id) {
                return false
            }
        }
        return true
    }

    /**
     * Compares this product's categories with the passed list, returns true only if both lists contain
     * the same categories
     */
    private fun isSameCategories(updatedCategories: List<ProductCategory>): Boolean {
        if (this.categories.size != updatedCategories.size) {
            return false
        }

        categories.forEach {
            if (!updatedCategories.containsCategory(it)) {
                return false
            }
        }
        return true
    }

    /**
     * Compares this product's tags with the passed list, returns true only if both lists contain
     * the same tags
     */
    private fun isSameTags(updatedTags: List<ProductTag>): Boolean {
        if (this.tags.size != updatedTags.size) {
            return false
        }

        tags.forEach {
            if (!updatedTags.containsTag(it)) {
                return false
            }
        }
        return true
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
            this.copy(
                description = updatedProduct.description,
                shortDescription = updatedProduct.shortDescription,
                name = updatedProduct.name,
                sku = updatedProduct.sku,
                slug = updatedProduct.slug,
                status = updatedProduct.status,
                catalogVisibility = updatedProduct.catalogVisibility,
                isFeatured = updatedProduct.isFeatured,
                isStockManaged = updatedProduct.isStockManaged,
                stockStatus = updatedProduct.stockStatus,
                stockQuantity = updatedProduct.stockQuantity,
                backorderStatus = updatedProduct.backorderStatus,
                isSoldIndividually = updatedProduct.isSoldIndividually,
                regularPrice = updatedProduct.regularPrice,
                salePrice = updatedProduct.salePrice,
                isVirtual = updatedProduct.isVirtual,
                isSaleScheduled = updatedProduct.isSaleScheduled,
                saleStartDateGmt = updatedProduct.saleStartDateGmt,
                saleEndDateGmt = updatedProduct.saleEndDateGmt,
                taxStatus = updatedProduct.taxStatus,
                taxClass = updatedProduct.taxClass,
                length = updatedProduct.length,
                width = updatedProduct.width,
                height = updatedProduct.height,
                weight = updatedProduct.weight,
                shippingClass = updatedProduct.shippingClass,
                images = updatedProduct.images,
                shippingClassId = updatedProduct.shippingClassId,
                reviewsAllowed = updatedProduct.reviewsAllowed,
                purchaseNote = updatedProduct.purchaseNote,
                externalUrl = updatedProduct.externalUrl,
                buttonText = updatedProduct.buttonText,
                menuOrder = updatedProduct.menuOrder,
                categories = updatedProduct.categories,
                tags = updatedProduct.tags,
                type = updatedProduct.type,
                groupedProductIds = updatedProduct.groupedProductIds
            )
        } ?: this.copy()
    }

    @StringRes
    fun getProductTypeFormattedForDisplay(): Int {
        return when (this.type) {
            ProductType.SIMPLE -> {
                if (this.isVirtual) R.string.product_type_virtual
                else R.string.product_type_physical
            }
            ProductType.VARIABLE -> R.string.product_type_variable
            ProductType.GROUPED -> R.string.product_type_grouped
            ProductType.EXTERNAL -> R.string.product_type_external
        }
    }
}

fun Product.toDataModel(storedProductModel: WCProductModel?): WCProductModel {
    fun imagesToJson(): String {
        val jsonArray = JsonArray()
        for (image in images) {
            jsonArray.add(JsonObject().also { json ->
                json.addProperty("id", image.id)
                json.addProperty("name", image.name)
                json.addProperty("source", image.source)
            })
        }
        return jsonArray.toString()
    }

    fun categoriesToJson(): String {
        val jsonArray = JsonArray()
        for (category in categories) {
            jsonArray.add(JsonObject().also { json ->
                json.addProperty("id", category.remoteCategoryId)
                json.addProperty("name", category.name)
                json.addProperty("slug", category.slug)
            })
        }
        return jsonArray.toString()
    }

    fun tagsToJson(): String {
        val jsonArray = JsonArray()
        for (tag in tags) {
            jsonArray.add(JsonObject().also { json ->
                json.addProperty("id", tag.remoteTagId)
                json.addProperty("name", tag.name)
                json.addProperty("slug", tag.slug)
            })
        }
        return jsonArray.toString()
    }

    return (storedProductModel ?: WCProductModel()).also {
        it.remoteProductId = remoteId
        it.description = description
        it.shortDescription = shortDescription
        it.name = name
        it.sku = sku
        it.slug = slug
        it.status = status.toString()
        it.catalogVisibility = catalogVisibility.toString()
        it.featured = isFeatured
        it.manageStock = isStockManaged
        it.stockStatus = ProductStockStatus.fromStockStatus(stockStatus)
        it.stockQuantity = stockQuantity
        it.soldIndividually = isSoldIndividually
        it.backorders = ProductBackorderStatus.fromBackorderStatus(backorderStatus)
        it.regularPrice = if (regularPrice.isNotSet()) "" else regularPrice.toString()
        it.salePrice = if (salePrice.isNotSet()) "" else salePrice.toString()
        it.length = if (length == 0f) "" else length.formatToString()
        it.width = if (width == 0f) "" else width.formatToString()
        it.weight = if (weight == 0f) "" else weight.formatToString()
        it.height = if (height == 0f) "" else height.formatToString()
        it.shippingClass = shippingClass
        it.taxStatus = ProductTaxStatus.fromTaxStatus(taxStatus)
        it.taxClass = taxClass
        it.images = imagesToJson()
        it.reviewsAllowed = reviewsAllowed
        it.virtual = isVirtual
        if (isSaleScheduled) {
            saleStartDateGmt?.let { dateOnSaleFrom ->
                it.dateOnSaleFromGmt = dateOnSaleFrom.formatToYYYYmmDDhhmmss()
            }
            it.dateOnSaleToGmt = saleEndDateGmt?.formatToYYYYmmDDhhmmss() ?: ""
        } else {
            it.dateOnSaleFromGmt = ""
            it.dateOnSaleToGmt = ""
        }
        it.purchaseNote = purchaseNote
        it.externalUrl = externalUrl
        it.buttonText = buttonText
        it.menuOrder = menuOrder
        it.categories = categoriesToJson()
        it.tags = tagsToJson()
        it.type = type.value
        it.groupedProductIds = groupedProductIds.joinToString(
            separator = ",",
            prefix = "[",
            postfix = "]"
        )
    }
}

fun WCProductModel.toAppModel(): Product {
    return Product(
        remoteId = this.remoteProductId,
        name = this.name,
        description = this.description,
        shortDescription = this.shortDescription,
        type = ProductType.fromString(this.type),
        status = ProductStatus.fromString(this.status),
        catalogVisibility = ProductCatalogVisibility.fromString(this.catalogVisibility),
        isFeatured = this.featured,
        stockStatus = ProductStockStatus.fromString(this.stockStatus),
        backorderStatus = ProductBackorderStatus.fromString(this.backorders),
        dateCreated = DateTimeUtils.dateFromIso8601(this.dateCreated) ?: Date(),
        firstImageUrl = this.getFirstImageUrl(),
        totalSales = this.totalSales,
        reviewsAllowed = this.reviewsAllowed,
        isVirtual = this.virtual,
        ratingCount = this.ratingCount,
        averageRating = this.averageRating.toFloatOrNull() ?: 0f,
        permalink = this.permalink,
        externalUrl = this.externalUrl,
        buttonText = this.buttonText,
        salePrice = this.salePrice.toBigDecimalOrNull()?.roundError(),
        regularPrice = this.regularPrice.toBigDecimalOrNull()?.roundError(),
        // In Core, if a tax class is empty it is considered as standard and we are following the same
        // procedure here
        taxClass = if (this.taxClass.isEmpty()) Product.TAX_CLASS_DEFAULT else this.taxClass,
        isStockManaged = this.manageStock,
        stockQuantity = this.stockQuantity,
        sku = this.sku,
        slug = this.slug,
        length = this.length.toFloatOrNull() ?: 0f,
        width = this.width.toFloatOrNull() ?: 0f,
        height = this.height.toFloatOrNull() ?: 0f,
        weight = this.weight.toFloatOrNull() ?: 0f,
        shippingClass = this.shippingClass,
        shippingClassId = this.shippingClassId.toLong(),
        isDownloadable = this.downloadable,
        fileCount = this.getDownloadableFiles().size,
        downloadLimit = this.downloadLimit,
        downloadExpiry = this.downloadExpiry,
        purchaseNote = this.purchaseNote,
        numVariations = this.getNumVariations(),
        images = this.getImages().map {
            Product.Image(
                it.id,
                it.name,
                it.src,
                DateTimeUtils.dateFromIso8601(this.dateCreated) ?: Date()
            )
        },
        attributes = this.getAttributes().map {
            Product.Attribute(
                it.id,
                it.name,
                it.options,
                it.visible
            )
        },
        saleEndDateGmt = this.dateOnSaleToGmt.formatDateToISO8601Format(),
        saleStartDateGmt = this.dateOnSaleFromGmt.formatDateToISO8601Format(),
        isSoldIndividually = this.soldIndividually,
        taxStatus = ProductTaxStatus.fromString(this.taxStatus),
        isSaleScheduled = this.dateOnSaleFromGmt.isNotEmpty() || this.dateOnSaleToGmt.isNotEmpty(),
        menuOrder = this.menuOrder,
        categories = this.getCategories().map {
            ProductCategory(
                it.id,
                it.name,
                it.slug
            )
        },
        tags = this.getTags().map {
            ProductTag(
                it.id,
                it.name,
                it.slug
            )
        },
        groupedProductIds = this.getGroupedProductIds()
    )
}

fun MediaModel.toAppModel(): Product.Image {
    return Product.Image(
        id = this.mediaId,
        name = this.fileName,
        source = this.url,
        dateCreated = DateTimeUtils.dateFromIso8601(this.uploadDate)
    )
}

fun List<Product>.isSameList(productList: List<Product>): Boolean {
    if (this.size != productList.size) {
        return false
    }
    for (index in this.indices) {
        val oldItem = productList[index]
        val newItem = this[index]
        if (!oldItem.isSameProduct(newItem)) {
            return false
        }
    }
    return true
}

/**
 * Returns the product as a [ProductReviewProduct] for use with the product reviews feature.
 */
fun WCProductModel.toProductReviewProductModel() =
    ProductReviewProduct(this.remoteProductId, this.name, this.permalink)
