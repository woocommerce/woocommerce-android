package com.woocommerce.android.ui.woopos.common.data

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.R
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper class responsible for all WooPosVariation conversions and transformations.
 * This centralizes all variation mapping logic, JSON parsing, and name generation.
 */
@Singleton
class WooPosVariationMapper @Inject constructor(
    private val gson: Gson
) {
    fun fromProductVariation(productVariation: ProductVariation): WooPosVariation {
        return WooPosVariation(
            remoteVariationId = productVariation.remoteVariationId,
            remoteProductId = productVariation.remoteProductId,
            globalUniqueId = productVariation.globalUniqueId,
            type = WooPosVariation.WooPosVariationType.VARIATION,
            price = productVariation.price,
            image = productVariation.image?.let { WooPosVariation.WooPosVariationImage(it.source) },
            attributes = productVariation.attributes.map {
                WooPosVariation.WooPosVariationAttribute(
                    id = it.id,
                    name = it.name,
                    option = it.option
                )
            },
            isVisible = productVariation.isVisible,
            isDownloadable = productVariation.isDownloadable
        )
    }

    @Suppress("SwallowedException")
    fun fromWCProductVariationModel(model: WCProductVariationModel): WooPosVariation {
        val attributesList = model.attributeList?.map { attribute ->
            WooPosVariation.WooPosVariationAttribute(
                id = attribute.id,
                name = attribute.name,
                option = attribute.option
            )
        } ?: emptyList()

        val imageModel = try {
            if (model.image.isNotEmpty()) model.getImageModel() else null
        } catch (e: JsonSyntaxException) {
            WooLog.w(
                WooLog.T.POS,
                "Failed to parse image from JSON attributes for variation " +
                    "${model.remoteVariationId.value}: ${e.message}"
            )
            null
        }

        return WooPosVariation(
            remoteVariationId = model.remoteVariationId.value,
            remoteProductId = model.remoteProductId.value,
            globalUniqueId = model.globalUniqueId,
            type = WooPosVariation.WooPosVariationType.VARIATION,
            price = model.price.toBigDecimalOrNull(),
            image = imageModel?.src?.let { WooPosVariation.WooPosVariationImage(it) },
            attributes = attributesList,
            isVisible = model.status == "publish",
            isDownloadable = model.downloadable
        )
    }

    @Suppress("SwallowedException")
    fun fromWooPosVariationEntity(model: WooPosVariationEntity): WooPosVariation {
        val attributesList = parseAttributesJson(model.attributesJson)

        return WooPosVariation(
            remoteVariationId = model.remoteVariationId.value,
            remoteProductId = model.remoteProductId.value,
            globalUniqueId = model.globalUniqueId,
            type = WooPosVariation.WooPosVariationType.fromValue(model.type),
            price = model.price.toBigDecimalOrNull(),
            image = if (model.imageUrl.isNotEmpty()) WooPosVariation.WooPosVariationImage(model.imageUrl) else null,
            attributes = attributesList,
            isVisible = model.status == "publish",
            isDownloadable = model.downloadable
        )
    }

    fun getNameForPOS(
        variation: WooPosVariation,
        parentProduct: WooPosProductModel? = null,
        resourceProvider: ResourceProvider,
    ): String {
        return parentProduct?.variationEnabledAttributes?.joinToString(", ") { attribute ->
            val option = variation.attributes.firstOrNull { it.name == attribute.name }
            if (option?.option != null) {
                "${attribute.name}: ${option.option}"
            } else {
                resourceProvider.getString(
                    R.string.woopos_variations_any_variation,
                    attribute.name
                )
            }
        } ?: variation.attributes.joinToString(", ") { attribute ->
            when {
                attribute.option != null && attribute.name != null -> "${attribute.name}: ${attribute.option}"
                attribute.option != null -> attribute.option
                attribute.name != null -> resourceProvider.getString(
                    R.string.woopos_variations_any_variation,
                    attribute.name
                )
                else -> ""
            }
        }
    }

    fun getName(variation: WooPosVariation, parentProduct: WooPosProductModel? = null): String {
        return parentProduct?.variationEnabledAttributes?.joinToString(" - ") { attribute ->
            val option = variation.attributes.firstOrNull { it.name == attribute.name }
            option?.option ?: "Any ${attribute.name}"
        } ?: variation.attributes.mapNotNull { it.option }.joinToString(" - ")
    }

    /**
     * Parses JSON string containing variation attributes into a list of WooPosVariationAttribute objects.
     *
     * @param attributesJson The JSON string to parse
     * @return List of parsed attributes, or empty list if parsing fails
     */
    private fun parseAttributesJson(attributesJson: String): List<WooPosVariation.WooPosVariationAttribute> {
        if (attributesJson.isEmpty()) return emptyList()

        return try {
            val type = object : TypeToken<List<AttributeJsonItem>>() {}.type
            val items: List<AttributeJsonItem> = gson.fromJson(attributesJson, type)
            items.map { item ->
                WooPosVariation.WooPosVariationAttribute(
                    id = item.id,
                    name = item.name,
                    option = item.option
                )
            }
        } catch (e: JsonSyntaxException) {
            WooLog.w(WooLog.T.POS, "Failed to parse attributes JSON: ${e.message}")
            emptyList()
        }
    }

    // @Keep: only instantiated reflectively by Gson via TypeToken, so R8 would otherwise
    // strip the class and Gson would fall back to LinkedTreeMap, causing a ClassCastException.
    @Keep
    private data class AttributeJsonItem(
        val id: Long?,
        val name: String?,
        val option: String?
    )
}

fun ProductVariation.toWooPosVariation(mapper: WooPosVariationMapper): WooPosVariation =
    mapper.fromProductVariation(this)

fun WCProductVariationModel.toWooPosVariation(mapper: WooPosVariationMapper): WooPosVariation =
    mapper.fromWCProductVariationModel(this)

fun WooPosVariationEntity.toWooPosVariation(mapper: WooPosVariationMapper): WooPosVariation =
    mapper.fromWooPosVariationEntity(this)

fun WooPosVariation.getNameForPOS(
    mapper: WooPosVariationMapper,
    parentProduct: WooPosProductModel? = null,
    resourceProvider: ResourceProvider,
): String = mapper.getNameForPOS(this, parentProduct, resourceProvider)

fun WooPosVariation.getName(mapper: WooPosVariationMapper, parentProduct: WooPosProductModel? = null): String =
    mapper.getName(this, parentProduct)
