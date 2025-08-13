package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel

/**
 * Represents a global attribute, which is an attribute available store-wide
 */
@Parcelize
data class ProductGlobalAttribute(
    val localSiteId: Int,
    val name: String,
    val slug: String,
    val type: String,
    val orderBy: String,
    val hasArchives: Boolean,
    val remoteId: Long
) : Parcelable {
    fun toDataModel(cachedAttribute: WCGlobalAttributeModel? = null): WCGlobalAttributeModel {
        return cachedAttribute ?: WCGlobalAttributeModel(
            siteId = LocalId(localSiteId),
            name = name,
            slug = slug,
            type = type,
            orderBy = orderBy,
            hasArchives = hasArchives,
            remoteId = RemoteId(remoteId),
        )
    }

    /**
     * Converts this global attribute to a "normal" product attribute model for display purposes only, useful when
     * listing global and local attributes together
     */
    fun toProductAttributeForDisplay(terms: List<String>): ProductAttribute {
        return ProductAttribute(
            id = this.remoteId,
            name = this.name,
            terms = terms,
            isVisible = ProductAttribute.DEFAULT_VISIBLE,
            isVariation = ProductAttribute.DEFAULT_IS_VARIATION
        )
    }
}

fun WCGlobalAttributeModel.toAppModel(): ProductGlobalAttribute {
    return ProductGlobalAttribute(
        remoteId = this.remoteId.value,
        localSiteId = this.siteId.value,
        name = this.name,
        slug = this.slug,
        type = this.type,
        orderBy = this.orderBy,
        hasArchives = this.hasArchives,
    )
}
