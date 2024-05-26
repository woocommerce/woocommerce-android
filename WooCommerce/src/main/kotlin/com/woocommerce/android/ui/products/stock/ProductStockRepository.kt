package com.woocommerce.android.ui.products.stock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductStockStatus.Companion.toCoreProductStockStatus
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
