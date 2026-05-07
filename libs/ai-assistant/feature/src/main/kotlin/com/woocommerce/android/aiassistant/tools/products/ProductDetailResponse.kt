package com.woocommerce.android.aiassistant.tools.products

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

internal fun WCProductModel.toProductDetailResponse(extraFields: Set<String> = emptySet()): ProductDetailResponse {
    val variationIds = getVariationIdList()
    val categoryList = getCategoryList()
    val attributeList = if ("attributes" in extraFields) getAttributeList() else emptyList()
    val imageList = getImageListOrEmpty()

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
        description = if ("description" in extraFields) description.take(PRODUCT_TEXT_FIELD_LIMIT) else null,
        descriptionTruncated = if ("description" in extraFields) description.length > PRODUCT_TEXT_FIELD_LIMIT else null,
        shortDescription = if ("short_description" in extraFields) {
            shortDescription.take(PRODUCT_TEXT_FIELD_LIMIT)
        } else {
            null
        },
        shortDescriptionTruncated = if ("short_description" in extraFields) {
            shortDescription.length > PRODUCT_TEXT_FIELD_LIMIT
        } else {
            null
        },
        attributes = if ("attributes" in extraFields) {
            attributeList.take(PRODUCT_ATTRIBUTES_LIMIT).map { it.toCompactProductAttribute() }
        } else {
            null
        },
        attributesTruncated = if ("attributes" in extraFields) attributeList.size > PRODUCT_ATTRIBUTES_LIMIT else null,
        images = if ("images" in extraFields) {
            imageList.take(PRODUCT_IMAGES_LIMIT).map { it.toCompactProductImage() }
        } else {
            null
        },
        imagesTruncated = if ("images" in extraFields) imageList.size > PRODUCT_IMAGES_LIMIT else null,
        dimensions = if ("dimensions" in extraFields) CompactProductDimensions(length, width, height) else null,
        weight = weight.takeIf { "weight" in extraFields },
        shippingClass = shippingClass.takeIf { "shipping_class" in extraFields },
        crossSellIds = getCrossSellProductIdList().takeIf { "cross_sell_ids" in extraFields },
        upsellIds = getUpsellProductIdList().takeIf { "upsell_ids" in extraFields },
        relatedIds = parseLongArray(relatedIds).takeIf { "related_ids" in extraFields },
    )
}

internal fun WCProductModel.toProductListRowResponse(extraFields: Set<String> = emptySet()): ProductListRowResponse =
    ProductListRowResponse(
        id = remoteProductId,
        name = name,
        sku = sku,
        price = price,
        stockStatus = stockStatus,
        type = type,
        status = status,
        regularPrice = regularPrice.takeIf { "regular_price" in extraFields },
        salePrice = salePrice.takeIf { "sale_price" in extraFields },
        onSale = onSale.takeIf { "on_sale" in extraFields },
        stockQuantity = stockQuantity.takeIf { "stock_quantity" in extraFields },
        manageStock = manageStock.takeIf { "manage_stock" in extraFields },
        categories = getCategoryList().map { it.toCompactProductTerm() }.takeIf { "categories" in extraFields },
        tags = getTagList().map { it.toCompactProductTerm() }.takeIf { "tags" in extraFields },
        totalSales = totalSales.takeIf { "total_sales" in extraFields },
        dateCreated = dateCreated.takeIf { "date_created" in extraFields },
        dateModified = dateModified.takeIf { "date_modified" in extraFields },
        image = getImageListOrEmpty().firstOrNull()?.toCompactProductImage().takeIf { "image" in extraFields },
        shortDescription = if ("short_description" in extraFields) {
            shortDescription.take(PRODUCT_TEXT_FIELD_LIMIT)
        } else {
            null
        },
        shortDescriptionTruncated = if ("short_description" in extraFields) {
            shortDescription.length > PRODUCT_TEXT_FIELD_LIMIT
        } else {
            null
        },
        description = if ("description" in extraFields) description.take(PRODUCT_TEXT_FIELD_LIMIT) else null,
        descriptionTruncated = if ("description" in extraFields) description.length > PRODUCT_TEXT_FIELD_LIMIT else null,
    )

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
    alt = alt.takeIf { it.isNotBlank() },
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
