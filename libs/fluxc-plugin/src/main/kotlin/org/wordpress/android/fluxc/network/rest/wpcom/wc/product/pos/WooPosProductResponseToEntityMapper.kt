package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos

import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.pos.WooPosVariationApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity

@Suppress("CyclomaticComplexMethod")
fun ProductApiResponse.mapToWooPOSEntity(localSiteId: LocalOrRemoteId.LocalId): WooPosProductEntity =
    WooPosProductEntity(
        localSiteId = localSiteId,
        remoteId = LocalOrRemoteId.RemoteId(this.id ?: 0),
        name = this.name ?: "",
        dateModified = this.date_modified ?: "",
        type = this.type ?: "",
        status = this.status ?: "",
        description = this.description ?: "",
        shortDescription = this.short_description ?: "",
        sku = this.sku ?: "",
        globalUniqueId = this.global_unique_id ?: "",
        price = this.price ?: "",
        regularPrice = this.regular_price ?: "",
        salePrice = this.sale_price ?: "",
        onSale = this.on_sale,
        downloadable = this.downloadable,
        manageStock = this.manage_stock?.let {
            it == "true" || it == "parent"
        } ?: false,
        stockQuantity = this.stock_quantity ?: 0.0,
        stockStatus = this.stock_status ?: "",
        backordersAllowed = this.backorders_allowed,
        backordered = this.backordered,
        parentId = this.parent_id,
        categories = this.categories?.toString() ?: "",
        tags = this.tags?.toString() ?: "",
        images = this.images?.toString() ?: "",
        attributes = this.attributes?.toString() ?: "",
    )

@Suppress("CyclomaticComplexMethod")
fun WooPosVariationApiResponse.mapToPosVariationModel(
    localSiteId: LocalOrRemoteId.LocalId
): WooPosVariationEntity {
    return WooPosVariationEntity(
        localSiteId = localSiteId,
        remoteProductId = LocalOrRemoteId.RemoteId(this.productId),
        remoteVariationId = LocalOrRemoteId.RemoteId(this.id),
        type = this.type ?: "",
        dateModified = this.dateModified ?: "",
        sku = this.sku ?: "",
        globalUniqueId = this.globalUniqueId ?: "",
        variationName = this.name ?: "",
        price = this.price ?: "",
        regularPrice = this.regularPrice ?: "",
        salePrice = this.salePrice ?: "",
        description = this.description ?: "",
        stockQuantity = this.stockQuantity ?: 0.0,
        stockStatus = this.stockStatus ?: "",
        manageStock = this.manageStock,
        backordered = this.backordered,
        attributesJson = this.attributesJson?.toString() ?: "[]",
        imageUrl = this.image?.src ?: "",
        status = this.status ?: "",
        downloadable = this.downloadable
    )
}
