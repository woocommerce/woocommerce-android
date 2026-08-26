package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.tools.truncated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.wordpress.android.fluxc.model.WCProductImageModel
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
    val categories: List<CompactProductTerm> = emptyList(),
    @SerialName("categories_count") val categoriesCount: Int = 0,
    @SerialName("categories_truncated") val categoriesTruncated: Boolean = false,
    @SerialName("total_sales") val totalSales: Long,
    @SerialName("parent_id") val parentId: Long,
    val permalink: String,
    @SerialName("variations_count") val variationsCount: Int,
    @SerialName("variation_ids") val variationIds: List<Long>,
    @SerialName("variation_ids_truncated") val variationIdsTruncated: Boolean,
    val description: String? = null,
    @SerialName("description_truncated") val descriptionTruncated: Boolean? = null,
    @SerialName("short_description") val shortDescription: String? = null,
    @SerialName("short_description_truncated") val shortDescriptionTruncated: Boolean? = null,
    val attributes: List<CompactProductAttribute>? = null,
    @SerialName("attributes_truncated") val attributesTruncated: Boolean? = null,
    val images: List<CompactProductImage>? = null,
    @SerialName("images_truncated") val imagesTruncated: Boolean? = null,
    val dimensions: CompactProductDimensions? = null,
    val weight: String? = null,
    @SerialName("shipping_class") val shippingClass: String? = null,
    @SerialName("cross_sell_ids") val crossSellIds: List<Long>? = null,
    @SerialName("upsell_ids") val upsellIds: List<Long>? = null,
    @SerialName("related_ids") val relatedIds: List<Long>? = null,
)

@Serializable
internal data class CompactProductTerm(
    val id: Long,
    val name: String,
    val slug: String,
)

@Serializable
internal data class CompactProductAttribute(
    val id: Long,
    val name: String,
    val visible: Boolean,
    val variation: Boolean,
    val options: List<String>,
)

@Serializable
internal data class CompactProductImage(
    val id: Long,
    val src: String? = null,
    val alt: String? = null,
    val name: String? = null,
)

@Serializable
internal data class CompactProductDimensions(
    val length: String,
    val width: String,
    val height: String,
)

@Serializable
internal data class ProductListRowResponse(
    val id: Long,
    val name: String,
    val sku: String,
    val price: String,
    @SerialName("stock_status") val stockStatus: String,
    val type: String,
    val status: String,
    @SerialName("regular_price") val regularPrice: String? = null,
    @SerialName("sale_price") val salePrice: String? = null,
    @SerialName("on_sale") val onSale: Boolean? = null,
    @SerialName("stock_quantity") val stockQuantity: Double? = null,
    @SerialName("manage_stock") val manageStock: Boolean? = null,
    val categories: List<CompactProductTerm>? = null,
    val tags: List<CompactProductTerm>? = null,
    @SerialName("total_sales") val totalSales: Long? = null,
    @SerialName("date_created") val dateCreated: String? = null,
    @SerialName("date_modified") val dateModified: String? = null,
    val image: CompactProductImage? = null,
    @SerialName("short_description") val shortDescription: String? = null,
    @SerialName("short_description_truncated") val shortDescriptionTruncated: Boolean? = null,
    val description: String? = null,
    @SerialName("description_truncated") val descriptionTruncated: Boolean? = null,
)

@Suppress("LongMethod")
internal fun WCProductModel.toProductDetailResponse(): ProductDetailResponse {
    val variationIds = getVariationIdList()
    val categoryList = safeCategoryList()
    val attributeList = safeAttributeList()
    val imageList = safeImageList()
    val compactDescription = description.truncated(PRODUCT_TEXT_FIELD_LIMIT)
    val compactShortDescription = shortDescription.truncated(PRODUCT_TEXT_FIELD_LIMIT)

    return ProductDetailResponse(
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
        categories = categoryList.take(PRODUCT_CATEGORIES_LIMIT).map { it.toCompactProductTerm() },
        categoriesCount = categoryList.size,
        categoriesTruncated = categoryList.size > PRODUCT_CATEGORIES_LIMIT,
        totalSales = totalSales,
        parentId = parentId,
        permalink = permalink,
        variationsCount = variationIds.size,
        variationIds = variationIds.take(PRODUCT_VARIATION_IDS_LIMIT),
        variationIdsTruncated = variationIds.size > PRODUCT_VARIATION_IDS_LIMIT,
        description = compactDescription.value,
        descriptionTruncated = compactDescription.truncated.takeIf { it },
        shortDescription = compactShortDescription.value,
        shortDescriptionTruncated = compactShortDescription.truncated.takeIf { it },
        attributes = attributeList.take(PRODUCT_ATTRIBUTES_LIMIT).map { it.toCompactProductAttribute() },
        attributesTruncated = attributeList.size > PRODUCT_ATTRIBUTES_LIMIT,
        images = imageList.take(PRODUCT_IMAGES_LIMIT).map { it.toCompactProductImage() },
        imagesTruncated = imageList.size > PRODUCT_IMAGES_LIMIT,
        dimensions = CompactProductDimensions(length, width, height),
        weight = weight,
        shippingClass = shippingClass,
        crossSellIds = safeCrossSellProductIdList(),
        upsellIds = safeUpsellProductIdList(),
        relatedIds = if (relatedIds.isNotBlank()) parseLongArray(relatedIds) else emptyList(),
    )
}

internal fun WCProductModel.toProductListRowResponse(): ProductListRowResponse {
    val compactDescription = description.truncated(PRODUCT_TEXT_FIELD_LIMIT)
    val compactShortDescription = shortDescription.truncated(PRODUCT_TEXT_FIELD_LIMIT)
    return ProductListRowResponse(
        id = remoteProductId,
        name = name,
        sku = sku,
        price = price,
        stockStatus = stockStatus,
        type = type,
        status = status,
        regularPrice = regularPrice,
        salePrice = salePrice,
        onSale = onSale,
        stockQuantity = stockQuantity,
        manageStock = manageStock,
        categories = safeCategoryList().map { it.toCompactProductTerm() },
        tags = safeTagList().map { it.toCompactProductTerm() },
        totalSales = totalSales,
        dateCreated = dateCreated,
        dateModified = dateModified,
        image = safeImageList().firstOrNull()?.toCompactProductImage(),
        shortDescription = compactShortDescription.value,
        shortDescriptionTruncated = compactShortDescription.truncated.takeIf { it },
        description = compactDescription.value,
        descriptionTruncated = compactDescription.truncated.takeIf { it },
    )
}

private fun WCProductModel.safeCategoryList() = if (categories.isBlank()) emptyList() else getCategoryList()

private fun WCProductModel.safeTagList() = if (tags.isBlank()) emptyList() else getTagList()

private fun WCProductModel.safeAttributeList() = if (attributes.isBlank()) emptyList() else getAttributeList()

private fun WCProductModel.safeImageList() = if (images.isBlank()) emptyList() else getImageListOrEmpty()

private fun WCProductModel.safeCrossSellProductIdList() =
    if (crossSellIds.isBlank()) emptyList() else getCrossSellProductIdList()

private fun WCProductModel.safeUpsellProductIdList() =
    if (upsellIds.isBlank()) emptyList() else getUpsellProductIdList()

private fun WCProductModel.ProductTriplet.toCompactProductTerm() = CompactProductTerm(
    id = id,
    name = name,
    slug = slug,
)

private fun WCProductModel.ProductAttribute.toCompactProductAttribute() = CompactProductAttribute(
    id = id,
    name = name,
    visible = visible,
    variation = variation,
    options = options,
)

internal fun WCProductImageModel.toCompactProductImage() = CompactProductImage(
    id = id,
    src = src.takeIf { it.isNotBlank() },
    alt = alt?.takeIf { it.isNotBlank() },
    name = name.takeIf { it.isNotBlank() },
)

private fun parseLongArray(jsonString: String): List<Long> = runCatching {
    PRODUCT_RESPONSE_JSON.parseToJsonElement(jsonString).jsonArray.mapNotNull { it.jsonPrimitive.longOrNull }
}.getOrDefault(emptyList())

private val PRODUCT_RESPONSE_JSON = Json { ignoreUnknownKeys = true }

internal const val PRODUCT_CATEGORIES_LIMIT = 5
internal const val PRODUCT_VARIATION_IDS_LIMIT = 20
internal const val PRODUCT_ATTRIBUTES_LIMIT = 10
internal const val PRODUCT_IMAGES_LIMIT = 3
internal const val PRODUCT_TEXT_FIELD_LIMIT = 500
