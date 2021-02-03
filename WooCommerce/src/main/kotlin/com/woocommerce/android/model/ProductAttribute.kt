package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeModel

@Parcelize
data class ProductAttribute(
    val id: Int,
    val localSiteId: Int,
    val name: String,
    val slug: String,
    val type: String,
    val orderBy: String,
    val hasArchives: Boolean,
    val termsId: String,
    val remoteId: Int
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        return (other as? ProductAttribute)?.let {
            id == it.id &&
                localSiteId == it.localSiteId &&
                name == it.name &&
                slug == it.slug &&
                type == it.type &&
                orderBy == it.orderBy &&
                hasArchives == it.hasArchives &&
                termsId == it.termsId &&
                remoteId == it.remoteId
        } ?: false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    fun toDataModel(cachedAttribute: WCProductAttributeModel? = null): WCProductAttributeModel {
        return (cachedAttribute ?: WCProductAttributeModel()).also {
            it.id = id
            it.localSiteId = localSiteId
            it.name = name
            it.slug = slug
            it.type = type
            it.orderBy = orderBy
            it.hasArchives = hasArchives
            it.termsId = termsId
            it.remoteId = remoteId
        }
    }
}

fun WCProductAttributeModel.toAppModel(): ProductAttribute {
    return ProductAttribute(
        id = this.id,
        remoteId = this.remoteId,
        localSiteId = this.localSiteId,
        name = this.name,
        slug = this.slug,
        type = this.type,
        orderBy = this.orderBy,
        hasArchives = this.hasArchives,
        termsId = this.termsId
    )
}
