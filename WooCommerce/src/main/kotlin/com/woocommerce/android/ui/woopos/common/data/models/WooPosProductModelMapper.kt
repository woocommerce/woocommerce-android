package com.woocommerce.android.ui.woopos.common.data.models

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import dagger.Reusable
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Mapper for converting WooPosProductEntity (database layer) to WooPosProductModel (domain layer).
 *
 * This is a read-only mapper since POS doesn't modify products.
 * The mapper ensures clean data handling:
 */
@Reusable
class WooPosProductModelMapper @Inject constructor(val logger: WooPosLogWrapper) {
    private val gson = Gson()
    fun fromEntity(entity: WooPosProductEntity): WooPosProductModel {
        return WooPosProductModel(
            remoteId = entity.remoteId.value,
            parentId = entity.parentId,
            name = entity.name,
            sku = entity.sku,
            globalUniqueId = entity.globalUniqueId,
            type = mapProductType(entity.type),
            status = mapProductStatus(entity.status),
            pricing = mapPricing(
                parsePriceOrNull(entity.price),
                parsePriceOrNull(entity.regularPrice),
                parsePriceOrNull(entity.salePrice),
                entity.onSale
            ),
            description = entity.description,
            shortDescription = entity.shortDescription,
            isDownloadable = entity.downloadable,
            images = parseImages(entity.images),
            attributes = parseAttributes(entity.attributes),
            categories = parseCategories(entity.categories),
            tags = parseTags(entity.tags),
            lastModified = entity.dateModified,
            variationIds = parseVariationIds(entity.variations)
        )
    }

    fun fromEntities(entities: List<WooPosProductEntity>): List<WooPosProductModel> {
        return entities.map { fromEntity(it) }
    }

    fun fromVariationEntity(entity: WooPosVariationEntity, parentProductName: String): WooPosProductModel {
        return WooPosProductModel(
            remoteId = entity.remoteVariationId.value,
            parentId = entity.remoteProductId.value,
            name = entity.variationName,
            sku = entity.sku,
            globalUniqueId = entity.globalUniqueId,
            type = WooPosProductModel.WooPosProductType.Variation,
            parentProductName = parentProductName,
            status = mapProductStatus(entity.status),
            pricing = mapPricing(
                parsePriceOrNull(entity.price),
                parsePriceOrNull(entity.regularPrice),
                parsePriceOrNull(entity.salePrice),
                entity.salePrice.isNotBlank()
            ),
            description = entity.description,
            shortDescription = "",
            isDownloadable = entity.downloadable,
            images = if (entity.imageUrl.isNotBlank()) {
                listOf(
                    WooPosProductModel.WooPosProductImage(
                        id = 0,
                        url = entity.imageUrl,
                        name = "",
                        alt = null
                    )
                )
            } else {
                emptyList()
            },
            attributes = parseAttributes(entity.attributesJson),
            lastModified = entity.dateModified,
        )
    }

    private fun parsePriceOrNull(priceString: String): BigDecimal? {
        return try {
            if (priceString.isBlank()) {
                null
            } else {
                BigDecimal(priceString)
            }
        } catch (e: NumberFormatException) {
            logger.e("Failed to parse price: '$priceString'", e)
            null
        }
    }

    fun parseImages(imagesJson: String): List<WooPosProductModel.WooPosProductImage> {
        return try {
            if (imagesJson.isBlank()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val imagesList: List<Map<String, Any?>> = gson.fromJson(imagesJson, type)
                imagesList.mapNotNull { imageMap ->
                    return@mapNotNull parseImage(imageMap)
                }
            }
        } catch (e: JsonSyntaxException) {
            logger.w("Failed to parse images JSON: $imagesJson - ${e.message}")
            emptyList()
        }
    }

    private fun parseImage(imageMap: Map<String, Any?>): WooPosProductModel.WooPosProductImage? {
        val id = when (val idValue = imageMap["id"]) {
            is Double -> idValue.toLong()
            is Int -> idValue.toLong()
            is Long -> idValue
            is String -> idValue.toLongOrNull()
            else -> null
        }
        val url = imageMap["src"] as? String
        val name = imageMap["name"] as? String ?: ""
        val alt = imageMap["alt"] as? String ?: ""
        if (id == null || url.isNullOrBlank()) {
            logger.w("Failed to parse images JSON, id or url is null: $id, $url")
            return null
        }
        return WooPosProductModel.WooPosProductImage(id, url, name, alt)
    }

    fun parseAttributes(attributesJson: String): List<WooPosProductModel.WooPosProductAttribute> {
        return try {
            if (attributesJson.isBlank()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val attributesList: List<Map<String, Any?>> = gson.fromJson(attributesJson, type)
                attributesList.mapNotNull { attrMap ->
                    return@mapNotNull parseAttribute(attrMap)
                }
            }
        } catch (e: JsonSyntaxException) {
            logger.w("Failed to parse attributes JSON: $attributesJson - ${e.message}")
            emptyList()
        }
    }

    private fun parseAttribute(attrMap: Map<String, Any?>): WooPosProductModel.WooPosProductAttribute? {
        val id = when (val idValue = attrMap["id"]) {
            is Double -> idValue.toLong()
            is Int -> idValue.toLong()
            is Long -> idValue
            is String -> idValue.toLongOrNull() ?: 0L
            else -> 0L
        }
        val name = attrMap["name"] as? String ?: return null
        val options = when (val optionsValue = attrMap["options"]) {
            is List<*> -> optionsValue.filterIsInstance<String>()
            is String -> listOf(optionsValue)
            else -> emptyList()
        }
        val isVisible = attrMap["visible"] as? Boolean ?: true
        val isVariation = attrMap["variation"] as? Boolean ?: false
        return WooPosProductModel.WooPosProductAttribute(id, name, options, isVisible, isVariation)
    }

    fun parseCategories(categoriesJson: String): List<WooPosProductModel.WooPosProductCategory> {
        return try {
            if (categoriesJson.isBlank()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val categoriesList: List<Map<String, Any?>> = gson.fromJson(categoriesJson, type)
                categoriesList.mapNotNull { catMap ->
                    return@mapNotNull parseCategory(catMap)
                }
            }
        } catch (e: JsonSyntaxException) {
            logger.w("Failed to parse categories JSON: $categoriesJson - ${e.message}")
            emptyList()
        }
    }

    private fun parseCategory(catMap: Map<String, Any?>): WooPosProductModel.WooPosProductCategory? {
        val id = when (val idValue = catMap["id"]) {
            is Double -> idValue.toLong()
            is Int -> idValue.toLong()
            is Long -> idValue
            is String -> idValue.toLongOrNull()
            else -> null
        }
        val name = catMap["name"] as? String
        val slug = catMap["slug"] as? String ?: ""
        if (id == null || name.isNullOrBlank()) {
            logger.w("Failed to parse category JSON, id or name is null: $id, $name")
            return null
        }
        return WooPosProductModel.WooPosProductCategory(id, name, slug)
    }

    fun parseTags(tagsJson: String): List<WooPosProductModel.WooPosProductTag> {
        return try {
            if (tagsJson.isBlank()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val tagsList: List<Map<String, Any?>> = gson.fromJson(tagsJson, type)
                tagsList.mapNotNull { tagMap ->
                    return@mapNotNull parseTag(tagMap)
                }
            }
        } catch (e: JsonSyntaxException) {
            logger.w("Failed to parse tags JSON: $tagsJson - ${e.message}")
            emptyList()
        }
    }

    private fun parseTag(tagMap: Map<String, Any?>): WooPosProductModel.WooPosProductTag? {
        val id = when (val idValue = tagMap["id"]) {
            is Double -> idValue.toLong()
            is Int -> idValue.toLong()
            is Long -> idValue
            is String -> idValue.toLongOrNull()
            else -> null
        }
        val name = tagMap["name"] as? String
        val slug = tagMap["slug"] as? String ?: ""
        if (id == null || name.isNullOrBlank()) {
            logger.w("Failed to parse tag JSON, id or name is null: $id, $name")
            return null
        }
        return WooPosProductModel.WooPosProductTag(id, name, slug)
    }

    private fun parseVariationIds(variationsJson: String): List<Long> {
        return try {
            if (variationsJson.isBlank()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<*>>() {}.type
                val variationsList: List<*> = gson.fromJson(variationsJson, type)
                variationsList.mapNotNull { variation ->
                    when (variation) {
                        is Double -> variation.toLong()
                        is Int -> variation.toLong()
                        is Long -> variation
                        is String -> variation.toLongOrNull()
                        else -> null
                    }
                }
            }
        } catch (e: JsonSyntaxException) {
            logger.w("Failed to parse variation IDs JSON: $variationsJson - ${e.message}")
            emptyList()
        }
    }

    fun mapPricing(
        price: BigDecimal?,
        regularPrice: BigDecimal?,
        salePrice: BigDecimal?,
        isOnSale: Boolean
    ): WooPosProductModel.WooPosPricing {
        return when {
            isOnSale && salePrice != null && regularPrice != null -> {
                WooPosProductModel.WooPosPricing.SalePricing(regularPrice, salePrice)
            }

            isOnSale && salePrice != null && price != null -> {
                WooPosProductModel.WooPosPricing.SalePricing(price, salePrice)
            }

            regularPrice != null -> {
                WooPosProductModel.WooPosPricing.RegularPricing(regularPrice)
            }

            price != null -> {
                WooPosProductModel.WooPosPricing.RegularPricing(price)
            }

            else -> WooPosProductModel.WooPosPricing.NoPricing
        }
    }

    fun mapProductType(type: String): WooPosProductModel.WooPosProductType {
        return when (type.lowercase()) {
            "simple" -> WooPosProductModel.WooPosProductType.Simple
            "variable" -> WooPosProductModel.WooPosProductType.Variable
            "grouped" -> WooPosProductModel.WooPosProductType.Grouped
            "external" -> WooPosProductModel.WooPosProductType.External
            "variation" -> WooPosProductModel.WooPosProductType.Variation
            "subscription" -> WooPosProductModel.WooPosProductType.Subscription
            "variable-subscription" -> WooPosProductModel.WooPosProductType.VariableSubscription
            "bundle" -> WooPosProductModel.WooPosProductType.Bundle
            "composite" -> WooPosProductModel.WooPosProductType.Composite
            else -> WooPosProductModel.WooPosProductType.Custom
        }
    }

    fun mapProductStatus(status: String): WooPosProductModel.WooPosProductStatus {
        return when (status.lowercase()) {
            "publish" -> WooPosProductModel.WooPosProductStatus.PUBLISH
            "draft" -> WooPosProductModel.WooPosProductStatus.DRAFT
            "pending" -> WooPosProductModel.WooPosProductStatus.PENDING
            "private" -> WooPosProductModel.WooPosProductStatus.PRIVATE
            "trash" -> WooPosProductModel.WooPosProductStatus.TRASH
            else -> WooPosProductModel.WooPosProductStatus.UNKNOWN
        }
    }
}
