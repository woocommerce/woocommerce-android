package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.ProductStockItems

@Parcelize
data class ProductStockItem(
    val productId: Long,
    val parentProductId: Int,
    val name: String,
    val stockQuantity: Int,
    val productThumbnail: String?,
    val itemsSold: Int?
) : Parcelable

fun ProductStockItems.toAppModel(): List<ProductStockItem> {
    return this.map {
        ProductStockItem(
            productId = it.productId ?: 0,
            parentProductId = it.parentId ?: 0,
            name = it.name ?: "",
            stockQuantity = it.stockQuantity ?: 0,
            productThumbnail = null, // TODO fetch product thumbnail
            itemsSold = null // TODO fetch items sold
        )
    }
}
