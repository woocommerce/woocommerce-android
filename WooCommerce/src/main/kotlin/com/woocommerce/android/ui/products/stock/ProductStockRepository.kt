package com.woocommerce.android.ui.products.stock

import android.os.Parcelable
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductStockStatus.Companion.toCoreProductStockStatus
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.ProductStockItems
import org.wordpress.android.fluxc.store.WCProductStockReportStore
import javax.inject.Inject

class ProductStockRepository @Inject constructor(
    private val store: WCProductStockReportStore,
    private val selectedSite: SelectedSite

) {
    suspend fun fetchProductStockReport(stockStatus: ProductStockStatus): Result<List<ProductStockItem>> {
        return store.fetchProductStockReport(
            site = selectedSite.get(),
            stockStatus = stockStatus.toCoreProductStockStatus()
        )
            .let { result ->
                if (result.isError) {
                    Result.failure(Exception(result.error.message))
                } else {
                    Result.success(result.model!!.toAppModel())
                }
            }
    }
}

@Parcelize
data class ProductStockItem(
    val productId: Long,
    val parentProductId: Int,
    val name: String,
    val stockQuantity: Int,
    val imageUrl: String?,
    val itemsSold: Int?
) : Parcelable

fun ProductStockItems.toAppModel(): List<ProductStockItem> {
    return this.map {
        ProductStockItem(
            productId = it.productId ?: 0,
            parentProductId = it.parentId ?: 0,
            name = it.name ?: "",
            stockQuantity = it.stockQuantity ?: 0,
            imageUrl = null, // TODO fetch product thumbnail
            itemsSold = null // TODO fetch items sold
        )
    }
}
