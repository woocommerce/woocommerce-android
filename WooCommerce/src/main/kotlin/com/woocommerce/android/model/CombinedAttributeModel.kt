package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * A "combined" product attribute is one that can be created from either a local (product-based) attribute
 * or a global (store-based) attribute
 */
@Parcelize
data class CombinedAttributeModel(
    val id: Long,
    val name: String
) : Parcelable {
    companion object {
        fun fromLocalAttribute(attribute: ProductAttribute): CombinedAttributeModel {
            return CombinedAttributeModel(
                id = attribute.id,
                name = attribute.name
            )
        }

        fun fromGlobalAttribute(attribute: ProductGlobalAttribute): CombinedAttributeModel {
            return CombinedAttributeModel(
                id = attribute.remoteId,
                name = attribute.name
            )
        }
    }
}
