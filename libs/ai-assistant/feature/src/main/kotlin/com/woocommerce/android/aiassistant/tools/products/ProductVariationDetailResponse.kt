package com.woocommerce.android.aiassistant.tools.products

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wordpress.android.fluxc.model.WCProductVariationModel

@Serializable
internal data class ProductVariationDetailResponse(
    val id: Long,
    @SerialName("product_id") val productId: Long,
    val status: String,
    val sku: String,
    @SerialName("regular_price") val regularPrice: String,
    @SerialName("sale_price") val salePrice: String,
    @SerialName("on_sale") val onSale: Boolean,
    @SerialName("manage_stock") val manageStock: Boolean,
    @SerialName("stock_quantity") val stockQuantity: Double,
    @SerialName("stock_status") val stockStatus: String,
    val price: String,
    val attributes: List<CompactVariationAttribute>,
    val image: CompactProductImage? = null,
    val description: String? = null,
    val weight: String? = null,
    val dimensions: CompactVariationDimensions? = null,
    @SerialName("tax_class") val taxClass: String? = null,
    @SerialName("date_created") val dateCreated: String? = null,
    @SerialName("date_modified") val dateModified: String? = null,
    @SerialName("menu_order") val menuOrder: Int? = null,
    val backorders: String? = null,
)

@Serializable
internal data class CompactVariationAttribute(
    val id: Long? = null,
    val name: String? = null,
    val option: String? = null,
)

@Serializable
internal data class CompactVariationDimensions(
    val length: String,
    val width: String,
    val height: String,
)

internal fun WCProductVariationModel.toProductVariationDetailResponse(
    extraFields: Set<String> = emptySet(),
) = ProductVariationDetailResponse(
    id = remoteVariationId.value,
    productId = remoteProductId.value,
    status = status,
    sku = sku,
    regularPrice = regularPrice,
    salePrice = salePrice,
    onSale = onSale,
    manageStock = manageStock,
    stockQuantity = stockQuantity,
    stockStatus = stockStatus,
    price = price,
    attributes = compactAttributes(),
    image = getImageModel()?.toCompactProductImage().takeIf { "image" in extraFields },
    description = description.takeIf { "description" in extraFields },
    weight = weight.takeIf { "weight" in extraFields },
    dimensions = CompactVariationDimensions(length, width, height).takeIf { "dimensions" in extraFields },
    taxClass = taxClass.takeIf { "tax_class" in extraFields },
    dateCreated = dateCreated.takeIf { "date_created" in extraFields },
    dateModified = dateModified.takeIf { "date_modified" in extraFields },
    menuOrder = menuOrder.takeIf { "menu_order" in extraFields },
    backorders = backorders.takeIf { "backorders" in extraFields },
)

private fun WCProductVariationModel.compactAttributes(): List<CompactVariationAttribute> =
    runCatching {
        attributeList.orEmpty().map { attribute ->
            CompactVariationAttribute(
                id = attribute.id,
                name = attribute.name,
                option = attribute.option,
            )
        }
    }.getOrDefault(emptyList())
