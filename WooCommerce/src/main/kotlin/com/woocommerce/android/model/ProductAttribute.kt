package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductModel

/**
 * Represents an attribute which is assigned to a product
 */
@Parcelize
data class ProductAttribute(
    val id: Long,
    val name: String,
    val terms: List<String>,
    val isVisible: Boolean
) : Parcelable {
    /**
     * Local attributes, which are attributes available only to a specific product, have an ID of zero
     */
    val isLocalAttribute: Boolean
        get() = id == 0L
}

fun WCProductModel.ProductAttribute.toAppModel(): ProductAttribute {
    return ProductAttribute(
        id = this.id,
        name = this.name,
        terms = this.options,
        isVisible = this.visible
    )
}
