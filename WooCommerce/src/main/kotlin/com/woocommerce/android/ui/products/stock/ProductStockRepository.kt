package com.woocommerce.android.ui.products.stock

import android.os.Parcelable
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductStockStatus.Companion.toCoreProductStockStatus
import com.woocommerce.android.util.DateUtils
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsProductApiResponse
import org.wordpress.android.fluxc.store.ProductStockItems
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCProductStockReportStore
import java.util.Date
import javax.inject.Inject

class ProductStockRepository @Inject constructor(
    private val stockReportStock: WCProductStockReportStore,
    private val leaderboardsStore: WCLeaderboardsStore,
    private val selectedSite: SelectedSite,
    private val dateUtils: DateUtils
) {
    suspend fun fetchProductStockReport(stockStatus: ProductStockStatus): Result<List<ProductStockItem>> {
        return stockReportStock.fetchProductStockReport(
            site = selectedSite.get(),
            stockStatus = stockStatus.toCoreProductStockStatus()
        ).let { stockReportResult ->
            if (stockReportResult.isError) {
                Result.failure(Exception(stockReportResult.error.message))
            } else {
                val productSalesResult = leaderboardsStore.fetchProductSalesReport(
                    site = selectedSite.get(),
                    startDate = Date(dateUtils.getCurrentDateTimeMinusDays(30)).formatToYYYYmmDDhhmmss(),
                    endDate = Date().formatToYYYYmmDDhhmmss(),
                    productIds = stockReportResult.model!!.map { stockReport ->
                        when {
                            stockReport.parentId != 0L -> stockReport.parentId!!
                            else -> stockReport.productId!!
                        }
                    },
                )
                if (productSalesResult.isError) {
                    return Result.failure(Exception(productSalesResult.error.message))
                } else {
                    val salesReport = productSalesResult.model!!
                    Result.success(
                        mapToProductStockItems(stockReportResult, salesReport)
                    )
                }
            }
        }
    }

    private fun mapToProductStockItems(
        result: WooResult<ProductStockItems>,
        salesReport: Array<ReportsProductApiResponse>
    ) = result.model!!.map {
        val productId = when {
            it.parentId != 0L -> it.parentId!!
            else -> it.productId!!
        }
        ProductStockItem(
            productId = it.productId ?: 0,
            parentProductId = it.parentId ?: 0,
            name = it.name ?: "",
            stockQuantity = it.stockQuantity ?: 0,
            itemsSold = getSalesForProduct(productId, salesReport),
            imageUrl = getProductThumbnail(productId, salesReport)
        )
    }

    private fun getSalesForProduct(
        productId: Long,
        fetchedSalesReport: Array<ReportsProductApiResponse>
    ): Int {
        return fetchedSalesReport
            .firstOrNull { it.productId == productId }
            ?.itemsSold ?: 0
    }

    private fun getProductThumbnail(productId: Long, fetchedSalesReport: Array<ReportsProductApiResponse>): String? {
        return fetchedSalesReport
            .firstOrNull { it.productId == productId }
            ?.product?.imageUrl
    }
}

@Parcelize
data class ProductStockItem(
    val productId: Long,
    val parentProductId: Long,
    val name: String,
    val stockQuantity: Int,
    val imageUrl: String?,
    val itemsSold: Int?
) : Parcelable
