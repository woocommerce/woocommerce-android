package com.woocommerce.android.ui.woopos.common.data

import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosVariationModel
import java.math.BigDecimal

/**
 * POS-specific product variation model containing only the fields needed for POS functionality.
 * This model provides better performance and cleaner separation compared to the general ProductVariation model.
 */
data class WooPosVariation(
    val remoteVariationId: Long,
    val remoteProductId: Long,
    val globalUniqueId: String,
    val price: BigDecimal?,
    val image: WooPosVariationImage?,
    val attributes: List<WooPosVariationAttribute>,
    val isVisible: Boolean,
    val isDownloadable: Boolean
) {

    data class WooPosVariationImage(
        val source: String
    )

    data class WooPosVariationAttribute(
        val id: Long?,
        val name: String?,
        val option: String?
    )
}


fun ProductVariation.toWooPosVariation(): WooPosVariation {
    return WooPosVariation(
        remoteVariationId = remoteVariationId,
        remoteProductId = remoteProductId,
        globalUniqueId = globalUniqueId,
        price = price,
        image = image?.let { WooPosVariation.WooPosVariationImage(it.source) },
        attributes = attributes.map {
            WooPosVariation.WooPosVariationAttribute(
                id = it.id,
                name = it.name,
                option = it.option
            )
        },
        isVisible = isVisible,
        isDownloadable = isDownloadable
    )
}

@Suppress("SwallowedException")
fun WCPosVariationModel.toWooPosVariation(): WooPosVariation {
    val attributesList = try {
        if (attributesJson.isNotEmpty()) {
            parseAttributesJson(attributesJson)
        } else {
            emptyList()
        }
    } catch (e: JsonSyntaxException) {
        emptyList()
    }

    return WooPosVariation(
        remoteVariationId = remoteVariationId.value,
        remoteProductId = remoteProductId.value,
        globalUniqueId = globalUniqueId,
        price = price.toBigDecimalOrNull(),
        image = if (imageUrl.isNotEmpty()) WooPosVariation.WooPosVariationImage(imageUrl) else null,
        attributes = attributesList,
        isVisible = status == "publish", // Convert status to visibility
        isDownloadable = downloadable
    )
}

@Suppress("SwallowedException")
private fun parseAttributesJson(attributesJson: String): List<WooPosVariation.WooPosVariationAttribute> {
    return try {
        val gson = com.google.gson.Gson()
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
        emptyList()
    }
}

private data class AttributeJsonItem(
    val id: Long?,
    val name: String?,
    val option: String?
)

fun WooPosVariation.getNameForPOS(
    parentProduct: Product? = null,
    resourceProvider: ResourceProvider,
): String {
    return parentProduct?.variationEnabledAttributes?.joinToString(", ") { attribute ->
        val option = attributes.firstOrNull { it.name == attribute.name }
        if (option?.option != null) {
            "${attribute.name}: ${option.option}"
        } else {
            resourceProvider.getString(
                R.string.woopos_variations_any_variation,
                attribute.name
            )
        }
    } ?: attributes.joinToString(", ") { attribute ->
        if (attribute.option != null) {
            "${attribute.name}: ${attribute.option}"
        } else {
            resourceProvider.getString(
                R.string.woopos_variations_any_variation,
                attribute.name!!
            )
        }
    }
}

fun WooPosVariation.getName(parentProduct: Product? = null): String {
    return parentProduct?.variationEnabledAttributes?.joinToString(" - ") { attribute ->
        val option = attributes.firstOrNull { it.name == attribute.name }
        option?.option ?: "Any ${attribute.name}"
    } ?: attributes.filter { it.option != null }.joinToString(" - ") { o -> o.option!! }
}
