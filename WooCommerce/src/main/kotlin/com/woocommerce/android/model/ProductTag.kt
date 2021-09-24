package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCProductTagModel

@Parcelize
data class ProductTag(
    val remoteTagId: Long = 0L,
    val name: String,
    val slug: String = "",
    val description: String = ""
) : Parcelable {
    /**
     * Adds tag to a product
     */
    fun addTag(product: Product?): List<ProductTag> {
        val selectedTags = product?.tags?.toMutableList() ?: mutableListOf()
        if (!selectedTags.contains(this)) { selectedTags.add(this) }
        return selectedTags
    }

    /**
     * Removes tag from a product
     */
    fun removeTag(product: Product?): List<ProductTag> {
        val selectedTags = product?.tags?.toMutableList() ?: mutableListOf()
        selectedTags.remove(this)
        return selectedTags
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

/**
 * Adds a list of tags to a product
 */
fun List<ProductTag>.addTags(product: Product?): List<ProductTag> {
    val selectedTags = product?.tags?.toMutableList() ?: mutableListOf()
    selectedTags.addAll(this)
    return selectedTags
}

fun WCProductTagModel.toProductTag(): ProductTag {
    return ProductTag(
        remoteTagId = this.remoteTagId,
        name = this.name,
        slug = this.slug,
        description = this.description
    )
}
