package com.woocommerce.android.ui.products.stock

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.ProductStockItems
import org.wordpress.android.fluxc.store.WCProductStockReportStore
import javax.inject.Inject

class ProductStockRepository @Inject constructor(
    private val store: WCProductStockReportStore,
    private val selectedSite: SelectedSite

) {

    suspend fun fetchProductStockReport(stockStatus: String): Result<List<ProductStockItem>> {
        return store.fetchProductStockReport(site = selectedSite.get(), stockStatus = stockStatus)
            .let { result ->
                if (result.isError) {
                    Result.failure(Exception(result.error.message))
                } else {
                    Result.success(result.model!!.toAppModel())
                }
            }
    }

    private fun ProductStockItems.toAppModel(): List<ProductStockItem> {
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

    data class ProductStockItem(
        val productId: Long,
        val parentProductId: Int,
        val name: String,
        val stockQuantity: Int,
        val productThumbnail: String?,
        val itemsSold: Int?
    )
}
