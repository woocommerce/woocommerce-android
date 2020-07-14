package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductTagModel

@Parcelize
data class ProductTag(
    val remoteTagId: Long = 0L,
    val name: String,
    val slug: String = "",
    val description: String = ""
) : Parcelable {
    fun toProductTag(): ProductTag {
        return ProductTag(
            this.remoteTagId,
            this.name,
            this.slug
        )
    }
}

/**
 * Returns true if the passed tag is in the current list of tags
 */
fun List<ProductTag>.containsTag(tag: ProductTag): Boolean {
    this.forEach {
        if (tag.remoteTagId == it.remoteTagId) {
            return true
        }
    }
    return false
}

fun WCProductTagModel.toProductTag(): ProductTag {
    return ProductTag(
        remoteTagId = this.remoteTagId,
        name = this.name,
        slug = this.slug,
        description = this.description
    )
}
