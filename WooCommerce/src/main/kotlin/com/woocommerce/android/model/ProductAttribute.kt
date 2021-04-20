package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCProductModel

/**
 * Represents an attribute which is assigned to a product
 */
@Parcelize
data class ProductAttribute(
    val id: Long,
    val name: String,
    val terms: List<String>,
    val isVisible: Boolean = DEFAULT_VISIBLE,
    val isVariation: Boolean = DEFAULT_IS_VARIATION
) : Parcelable {
    companion object {
        val DEFAULT_VISIBLE = true
        val DEFAULT_IS_VARIATION = true
    }

    /**
     * Local attributes, which are attributes available only to a specific product, have an ID of zero
     */
    val isLocalAttribute: Boolean
        get() = id == 0L

    val isGlobalAttribute: Boolean
        get() = !isLocalAttribute

    fun toDataModel() =
        WCProductModel.ProductAttribute(
            id = id,
            name = name,
            options = terms.toMutableList(),
            visible = isVisible,
            variation = isVariation
        )
}

fun WCProductModel.ProductAttribute.toAppModel(): ProductAttribute {
    return ProductAttribute(
        id = this.id,
        name = this.name,
        terms = this.options,
        isVisible = this.visible,
        isVariation = this.variation
    )
}
