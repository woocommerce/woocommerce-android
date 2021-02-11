package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductGlobalAttribute

/**
 * A "combined" product attribute is one that can be created from either a local (product-based) attribute
 * or a global (store-based) attribute
 */
data class CombinedAttributeModel(
    val id: Long,
    val name: String
) {
    companion object {
        fun fromLocalAttribute(attribute: Product.Attribute): CombinedAttributeModel {
            return CombinedAttributeModel(
                id = attribute.id,
                name = attribute.name
            )
        }

        fun fromGlobalAttribute(attribute: ProductGlobalAttribute): CombinedAttributeModel {
            return CombinedAttributeModel(
                id = attribute.id.toLong(),
                name = attribute.name
            )
        }
    }

    val isGlobalAttribute: Boolean
        get() = id != 0L
}
