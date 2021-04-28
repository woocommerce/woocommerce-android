package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.attribute.terms.WCAttributeTermModel

/**
 * Represents an attribute term, ex: attribute "Color" could have a "Blue" term (aka option)
 */
@Parcelize
data class ProductAttributeTerm(
    val id: Int,
    val remoteId: Int,
    val name: String,
    val slug: String,
    val description: String
) : Parcelable

fun WCAttributeTermModel.toAppModel(): ProductAttributeTerm {
    return ProductAttributeTerm(
        id = this.attributeId,
        remoteId = this.remoteId,
        name = this.name,
        slug = this.slug,
        description = this.description
    )
}
