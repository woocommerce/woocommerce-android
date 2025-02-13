package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.get
import org.wordpress.android.fluxc.utils.JsonElementToLongSerializerDeserializer

data class LineItem(
    @JsonAdapter(JsonElementToLongSerializerDeserializer::class)
    val id: Long? = null,
    val name: String? = null,
    @SerializedName("parent_name")
    val parentName: String? = null,
    @SerializedName("product_id")
    val productId: Long? = null,
    @SerializedName("variation_id")
    val variationId: Long? = null,
    val quantity: Float? = null,
    val subtotal: String? = null,
    val total: String? = null, // Price x quantity
    @SerializedName("total_tax")
    val totalTax: String? = null,
    val sku: String? = null,
    val price: String? = null, // The per-item price
    @SerializedName("meta_data")
    val metaData: List<WCMetaData>? = null,
    @SerializedName("bundled_by")
    val bundledBy: String? = null,
    @SerializedName("composite_parent")
    val compositeParent: String? = null
) {
    class Attribute(val key: String?, val value: String?)

    fun getAttributeList(): List<Attribute> {
        return metaData?.filter {
            it.displayKey is String && it.displayValue?.isPrimitive == true
        }?.map {
            Attribute(it.displayKey, it.displayValue?.stringValue)
        } ?: emptyList()
    }

    val configurationKey
        get() = bundledBy.takeIf { it.isNullOrBlank().not() }
            ?.let { metaData?.get(WCMetaData.BundleMetadataKeys.BUNDLED_ITEM_ID) }
            ?.valueAsString
            ?.toLongOrNull()
}
