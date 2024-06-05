package com.woocommerce.android.ui.products.stock

import android.os.Parcelable
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductStockStatus.Companion.toCoreProductStockStatus
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsProductApiResponse
import org.wordpress.android.fluxc.store.ProductStockItems
import org.wordpress.android.fluxc.store.WCProductReportsStore
import java.util.Date
import javax.inject.Inject

class ProductStockRepository @Inject constructor(
    private val stockReportStore: WCProductReportsStore,
    private val selectedSite: SelectedSite,
    private val dateUtils: DateUtils
) {
    companion object {
        private const val DAYS_TO_FETCH = 30
    }

    suspend fun fetchProductStockReport(stockStatus: ProductStockStatus): Result<List<ProductStockItem>> {
        return stockReportStore.fetchProductStockReport(
            site = selectedSite.get(),
            stockStatus = stockStatus.toCoreProductStockStatus()
        ).let { stockReportResult ->
            if (stockReportResult.isError) {
                Result.failure(Exception(stockReportResult.error.message))
            } else {
                val (productSalesResult, variationSalesResult) = getProductSalesReports(stockReportResult.model!!)
                return when {
                    productSalesResult.isError -> Result.failure(Exception(productSalesResult.error.message))
                    variationSalesResult.isError -> Result.failure(Exception(variationSalesResult.error.message))
                    else ->
                        Result.success(
                            mapToProductStockItems(
                                stockReportResult.model!!,
                                productSalesResult.model!! + variationSalesResult.model!!
                            )
                        )
                }
            }
        }
    }

    private suspend fun getProductSalesReports(stockReport: ProductStockItems):
        Pair<WooResult<Array<ReportsProductApiResponse>>, WooResult<Array<ReportsProductApiResponse>>> {
        val startDate = Date(dateUtils.getCurrentDateTimeMinusDays(DAYS_TO_FETCH)).formatToYYYYmmDDhhmmss()
        val endDate = Date().formatToYYYYmmDDhhmmss()
        return coroutineScope {
            val productSalesDeferred = async {
                getProductSales(
                    stockReport
                        .filter { it.parentId == 0L }
                        .mapNotNull { it.productId },
                    startDate,
                    endDate
                )
            }
            val variationSalesDeferred = async {
                getVariationsSales(
                    stockReport
                        .filter { it.parentId != 0L }
                        .mapNotNull { it.productId },
                    startDate,
                    endDate
                )
            }
            Pair(productSalesDeferred.await(), variationSalesDeferred.await())
        }
    }

    private suspend fun getProductSales(productIds: List<Long>, startDate: String, endDate: String) =
        when {
            productIds.isEmpty() -> WooResult(emptyArray())
            else -> stockReportStore.fetchProductSalesReport(
                site = selectedSite.get(),
                startDate = startDate,
                endDate = endDate,
                productIds = productIds,
            )
        }

    private suspend fun getVariationsSales(variationIds: List<Long>, startDate: String, endDate: String) =
        when {
            variationIds.isEmpty() -> WooResult(emptyArray())
            else -> stockReportStore.fetchProductVariationsSalesReport(
                site = selectedSite.get(),
                startDate = startDate,
                endDate = endDate,
                productVariationIds = variationIds,
            )
        }

    private fun mapToProductStockItems(
        stockReport: ProductStockItems,
        salesReport: Array<ReportsProductApiResponse>
    ) = stockReport.map {
        ProductStockItem(
            productId = it.productId ?: 0,
            parentProductId = it.parentId ?: 0,
            name = it.name ?: "",
            stockQuantity = it.stockQuantity ?: 0,
            itemsSold = getSalesForProduct(it.productId, salesReport),
            imageUrl = getProductThumbnail(it.productId, salesReport)
        )
    }

    private fun getSalesForProduct(
        productId: Long?,
        fetchedSalesReport: Array<ReportsProductApiResponse>
    ): Int {
        return fetchedSalesReport
            .firstOrNull { it.variationId == productId || it.productId == productId }
            ?.itemsSold ?: 0
    }

    private fun getProductThumbnail(
        productId: Long?,
        fetchedSalesReport: Array<ReportsProductApiResponse>
    ): String? {
        return fetchedSalesReport
            .firstOrNull { it.variationId == productId || it.productId == productId }
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
    val itemsSold: Int
) : Parcelable
