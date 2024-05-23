package com.woocommerce.android.ui.products.stock

import com.woocommerce.android.model.ProductStockItem
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
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
}
