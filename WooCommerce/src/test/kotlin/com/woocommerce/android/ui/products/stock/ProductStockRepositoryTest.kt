package com.woocommerce.android.ui.products.stock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductStockStatus.Companion.toCoreProductStockStatus
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ProductStockItemApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsProductApiResponse
import org.wordpress.android.fluxc.store.ProductStockItems
import org.wordpress.android.fluxc.store.WCProductReportsStore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductStockRepositoryTest : BaseUnitTest() {
    private val stockReportStock: WCProductReportsStore = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel().apply {
            url = "https://woosite.com"
        }
    }
    private val dateUtils: DateUtils = mock {
        on { getCurrentDateTimeMinusDays(30) } doReturn ANY_START_DATE_IN_MILLIS
    }

    private var productStockRepository = ProductStockRepository(
        stockReportStore = stockReportStock,
        selectedSite = selectedSite,
        dateUtils = dateUtils
    )

    @Test
    fun `when fetching stock fails, then sales are not fetched and error is returned`() = testBlocking {
        givenFetchProductStockReturnsError(LOW_STOCK_STATUS, WooError(GENERIC_ERROR, UNKNOWN))

        val result = productStockRepository.fetchProductStockReport(LOW_STOCK_STATUS)

        verify(stockReportStock, never()).fetchProductSalesReport(any(), any(), any(), any())
        verify(stockReportStock, never()).fetchProductVariationsSalesReport(any(), any(), any(), any())
        assertTrue(result.isFailure)
    }

    @Test
    fun `given fetching stock succeeds, when fetching product sales, stock and sales are merged successfully`() =
        testBlocking {
            givenFetchProductStockReturns(LOW_STOCK_STATUS, PRODUCT_STOCK_ITEMS)
            givenFetchProductSalesSuccess()

            val result = productStockRepository.fetchProductStockReport(LOW_STOCK_STATUS)

            assertEquals(STOCK_AND_SALES_REPORT, result.getOrNull())
        }

    @Test
    fun `given product stock with only products, when fetching product sales, fetch only sales for products`() =
        testBlocking {
            givenFetchProductStockReturns(LOW_STOCK_STATUS, PRODUCT_STOCK_ITEMS)
            givenFetchProductSalesSuccess()

            productStockRepository.fetchProductStockReport(LOW_STOCK_STATUS)

            verify(stockReportStock).fetchProductSalesReport(any(), any(), any(), any())
            verify(stockReportStock, never()).fetchProductVariationsSalesReport(any(), any(), any(), any())
        }

    @Test
    fun `given product stock with only variations, when fetching product sales, fetch only sales for variations`() =
        testBlocking {
            givenFetchProductStockReturns(LOW_STOCK_STATUS, PRODUCT_STOCK_ITEMS_WITH_ONLY_VARIATIONS)
            givenFetchProductVariationSalesSuccess()

            productStockRepository.fetchProductStockReport(LOW_STOCK_STATUS)

            verify(stockReportStock).fetchProductVariationsSalesReport(any(), any(), any(), any())
            verify(stockReportStock, never()).fetchProductSalesReport(any(), any(), any(), any())
        }

    @Test
    fun `given product stock with variations and products, when fetching product sales, fetch sales for both types`() =
        testBlocking {
            givenFetchProductStockReturns(LOW_STOCK_STATUS, PRODUCT_STOCK_ITEMS_WITH_VARIATIONS)
            givenFetchProductSalesSuccess()
            givenFetchProductVariationSalesSuccess()

            val result = productStockRepository.fetchProductStockReport(LOW_STOCK_STATUS)

            verify(stockReportStock).fetchProductSalesReport(any(), any(), any(), any())
            verify(stockReportStock).fetchProductVariationsSalesReport(any(), any(), any(), any())
            assertEquals(STOCK_AND_SALES_REPORT_WITH_VARIATION, result.getOrNull())
        }

    @Test
    fun `given stock is cached, when fetching stock again, don't fetch sales if stock report has not changed`() =
        testBlocking {
            givenFetchProductStockReturns(LOW_STOCK_STATUS, PRODUCT_STOCK_ITEMS_WITH_VARIATIONS)
            givenFetchProductSalesSuccess()
            givenFetchProductVariationSalesSuccess()

            productStockRepository.fetchProductStockReport(LOW_STOCK_STATUS) // First fetch to cache stock
            productStockRepository.fetchProductStockReport(LOW_STOCK_STATUS)

            verify(stockReportStock, times(2)).fetchProductStockReport(any(), any(), any(), any())
            verify(stockReportStock, times(1)).fetchProductSalesReport(any(), any(), any(), any())
            verify(stockReportStock, times(1)).fetchProductVariationsSalesReport(any(), any(), any(), any())
        }

    private suspend fun givenFetchProductStockReturnsError(stockStatus: ProductStockStatus, wooError: WooError) {
        whenever(
            stockReportStock.fetchProductStockReport(
                selectedSite.get(),
                stockStatus.toCoreProductStockStatus()
            )
        ).thenReturn(WooResult(wooError))
    }

    private suspend fun givenFetchProductStockReturns(stockStatus: ProductStockStatus, stockItems: ProductStockItems) {
        whenever(
            stockReportStock.fetchProductStockReport(
                selectedSite.get(),
                stockStatus.toCoreProductStockStatus()
            )
        ).thenReturn(WooResult(stockItems))
    }

    private suspend fun givenFetchProductSalesSuccess() {
        whenever(
            stockReportStock.fetchProductSalesReport(
                any(),
                any(),
                any(),
                eq(PRODUCT_STOCK_ITEMS.map { it.productId!! })
            )
        ).thenReturn(WooResult(PRODUCT_SALES_REPORT))
    }

    private suspend fun givenFetchProductVariationSalesSuccess() {
        whenever(
            stockReportStock.fetchProductVariationsSalesReport(
                any(),
                any(),
                any(),
                eq(PRODUCT_STOCK_ITEMS_WITH_ONLY_VARIATIONS.map { it.productId!! })
            )
        ).thenReturn(WooResult(PRODUCT_VARIATION_SALES_REPORT))
    }

    private companion object {
        val LOW_STOCK_STATUS = ProductStockStatus.LowStock
        const val ANY_START_DATE_IN_MILLIS = 1704063600L
        val PRODUCT_VARIATION_STOCK = ProductStockItemApiResponse(
            productId = 2,
            parentId = 1,
            stockQuantity = 1,
            stockStatus = "instock",
            name = "Product 1 Variation",
        )
        val PRODUCT_STOCK_ITEMS: ProductStockItems = arrayOf(
            ProductStockItemApiResponse(
                productId = 1,
                parentId = 0,
                name = "Product 1",
                stockQuantity = 1,
                stockStatus = "instock",
            )
        )
        val PRODUCT_STOCK_ITEMS_WITH_VARIATIONS: ProductStockItems = arrayOf(
            ProductStockItemApiResponse(
                productId = 1,
                parentId = 0,
                name = "Product 1",
                stockQuantity = 1,
                stockStatus = "instock",
            ),
            PRODUCT_VARIATION_STOCK
        )
        val PRODUCT_STOCK_ITEMS_WITH_ONLY_VARIATIONS: ProductStockItems = arrayOf(PRODUCT_VARIATION_STOCK)
        val PRODUCT_SALES_REPORT = arrayOf(
            ReportsProductApiResponse(
                productId = 1,
                variationId = null,
                itemsSold = 2,
            ),
        )
        val PRODUCT_VARIATION_SALES_REPORT = arrayOf(
            ReportsProductApiResponse(
                productId = 1,
                variationId = 2,
                itemsSold = 2,
            ),
        )
        val STOCK_AND_SALES_REPORT = listOf(
            ProductStockItem(
                productId = 1,
                parentProductId = 0,
                name = "Product 1",
                stockQuantity = 1,
                itemsSold = 2,
                imageUrl = null
            )
        )
        val STOCK_AND_SALES_REPORT_WITH_VARIATION = listOf(
            ProductStockItem(
                productId = 1,
                parentProductId = 0,
                name = "Product 1",
                stockQuantity = 1,
                itemsSold = 2,
                imageUrl = null
            ),
            ProductStockItem(
                productId = 2,
                parentProductId = 1,
                name = "Product 1 Variation",
                stockQuantity = 1,
                itemsSold = 2,
                imageUrl = null
            )
        )
    }
}
