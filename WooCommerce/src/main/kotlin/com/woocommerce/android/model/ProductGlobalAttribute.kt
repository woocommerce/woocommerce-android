package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel

/**
 * Represents a global attribute, which is an attribute available store-wide
 */
@Parcelize
data class ProductGlobalAttribute(
    val id: Int,
    val localSiteId: Int,
    val name: String,
    val slug: String,
    val type: String,
    val orderBy: String,
    val hasArchives: Boolean,
    val termsId: String,
    val remoteId: Long
) : Parcelable {
    fun toDataModel(cachedAttribute: WCGlobalAttributeModel? = null): WCGlobalAttributeModel {
        return (cachedAttribute ?: WCGlobalAttributeModel()).also {
            it.id = id
            it.localSiteId = localSiteId
            it.name = name
            it.slug = slug
            it.type = type
            it.orderBy = orderBy
            it.hasArchives = hasArchives
            it.termsId = termsId
            it.remoteId = remoteId.toInt()
        }
    }

    /**
     * Converts this global attribute to a "normal" product attribute model for display purposes only (since the
     * list of terms will always be empty and isVisible may not be correct), useful when listing global and local
     * attributes together
     */
    fun toProductAttributeForDisplay(): ProductAttribute {
        return ProductAttribute(
            id = this.remoteId,
            name = this.name,
            terms = emptyList(),
            isVisible = true
        )
    }
}

fun WCGlobalAttributeModel.toAppModel(): ProductGlobalAttribute {
    return ProductGlobalAttribute(
        id = this.id,
        remoteId = this.remoteId.toLong(),
        localSiteId = this.localSiteId,
        name = this.name,
        slug = this.slug,
        type = this.type,
        orderBy = this.orderBy,
        hasArchives = this.hasArchives,
        termsId = this.termsId
    )
}
