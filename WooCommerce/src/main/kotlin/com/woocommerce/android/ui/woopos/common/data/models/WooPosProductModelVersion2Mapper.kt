package com.woocommerce.android.ui.woopos.common.data.models

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import dagger.Reusable
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductEntity
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Mapper for converting WCPosProductEntity (database layer) to WooPosProductModel (domain layer).
 *
 * This is a read-only mapper since POS doesn't modify products.
 * The mapper ensures clean data handling:
 */
@Reusable
class WooPosProductModelVersion2Mapper @Inject constructor(val logger: WooPosLogWrapper) {
    private val gson = Gson()
    fun fromEntity(entity: WCPosProductEntity): WooPosProductModelVersion2 {
        return WooPosProductModelVersion2(
            id = entity.remoteId.value,
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
            lastModified = entity.dateModified
        )
    }

    fun fromEntities(entities: List<WCPosProductEntity>): List<WooPosProductModelVersion2> {
        return entities.map { fromEntity(it) }
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

    private fun parseImages(imagesJson: String): List<WooPosProductModelVersion2.WooPosProductImage> {
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

    private fun parseImage(imageMap: Map<String, Any?>): WooPosProductModelVersion2.WooPosProductImage? {
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
        return WooPosProductModelVersion2.WooPosProductImage(id, url, name, alt)
    }

    private fun parseAttributes(attributesJson: String): List<WooPosProductModelVersion2.WooPosProductAttribute> {
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

    private fun parseAttribute(attrMap: Map<String, Any?>): WooPosProductModelVersion2.WooPosProductAttribute? {
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
        return WooPosProductModelVersion2.WooPosProductAttribute(id, name, options, isVisible, isVariation)
    }

    private fun parseCategories(categoriesJson: String): List<WooPosProductModelVersion2.WooPosProductCategory> {
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

    private fun parseCategory(catMap: Map<String, Any?>): WooPosProductModelVersion2.WooPosProductCategory? {
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
        return WooPosProductModelVersion2.WooPosProductCategory(id, name, slug)
    }

    private fun parseTags(tagsJson: String): List<WooPosProductModelVersion2.WooPosProductTag> {
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

    private fun parseTag(tagMap: Map<String, Any?>): WooPosProductModelVersion2.WooPosProductTag? {
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
        return WooPosProductModelVersion2.WooPosProductTag(id, name, slug)
    }

    fun mapPricing(
        price: BigDecimal?,
        regularPrice: BigDecimal?,
        salePrice: BigDecimal?,
        isOnSale: Boolean
    ): WooPosProductModelVersion2.WooPosPricing {
        return when {
            isOnSale && salePrice != null && regularPrice != null -> {
                WooPosProductModelVersion2.WooPosPricing.SalePricing(regularPrice, salePrice)
            }

            isOnSale && salePrice != null && price != null -> {
                WooPosProductModelVersion2.WooPosPricing.SalePricing(price, salePrice)
            }

            regularPrice != null -> {
                WooPosProductModelVersion2.WooPosPricing.RegularPricing(regularPrice)
            }

            price != null -> {
                WooPosProductModelVersion2.WooPosPricing.RegularPricing(price)
            }

            else -> WooPosProductModelVersion2.WooPosPricing.NoPricing
        }
    }

    fun mapProductType(type: String): WooPosProductModelVersion2.WooPosProductType {
        return when (type.lowercase()) {
            "simple" -> WooPosProductModelVersion2.WooPosProductType.SIMPLE
            "variable" -> WooPosProductModelVersion2.WooPosProductType.VARIABLE
            "grouped" -> WooPosProductModelVersion2.WooPosProductType.GROUPED
            "external" -> WooPosProductModelVersion2.WooPosProductType.EXTERNAL
            "variation" -> WooPosProductModelVersion2.WooPosProductType.VARIATION
            "subscription" -> WooPosProductModelVersion2.WooPosProductType.SUBSCRIPTION
            "variable-subscription" -> WooPosProductModelVersion2.WooPosProductType.VARIABLE_SUBSCRIPTION
            "bundle" -> WooPosProductModelVersion2.WooPosProductType.BUNDLE
            "composite" -> WooPosProductModelVersion2.WooPosProductType.COMPOSITE
            else -> WooPosProductModelVersion2.WooPosProductType.CUSTOM
        }
    }

    fun mapProductStatus(status: String): WooPosProductModelVersion2.WooPosProductStatus {
        return when (status.lowercase()) {
            "publish" -> WooPosProductModelVersion2.WooPosProductStatus.PUBLISH
            "draft" -> WooPosProductModelVersion2.WooPosProductStatus.DRAFT
            "pending" -> WooPosProductModelVersion2.WooPosProductStatus.PENDING
            "private" -> WooPosProductModelVersion2.WooPosProductStatus.PRIVATE
            "trash" -> WooPosProductModelVersion2.WooPosProductStatus.TRASH
            else -> WooPosProductModelVersion2.WooPosProductStatus.UNKNOWN
        }
    }
}
