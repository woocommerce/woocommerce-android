package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.ProductGlobalAttribute

/**
 * A "combined" product attribute is one that can be created from either a local (product-based) attribute
 * or a global (store-based) attribute
 */
class CombinedAttributeModel(
    val id: Long,
    val name: String
) {
    companion object {
        fun fromLocalAttribute(attribute: ProductAttribute): CombinedAttributeModel {
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

    override fun equals(other: Any?): Boolean {
        return (other as? CombinedAttributeModel)?.let {
            id == it.id &&
                name == it.name
        } ?: false
    }

    override fun hashCode() = super.hashCode()

    fun isGlobalAttribute() = id != 0L
}
