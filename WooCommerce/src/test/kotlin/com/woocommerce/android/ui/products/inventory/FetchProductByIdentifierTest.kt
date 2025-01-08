package com.woocommerce.android.ui.products.inventory

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.creation.CheckDigitRemover
import com.woocommerce.android.ui.orders.creation.CheckDigitRemoverFactory
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductTestUtils.generateProductList
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FetchProductByIdentifierTest : BaseUnitTest() {
    private val repo: ProductListRepository = mock()
    private val checkDigitRemoverFactory: CheckDigitRemoverFactory = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val sut = FetchProductByIdentifier(repo, checkDigitRemoverFactory, analyticsTracker)

    @Test
    fun `given barcode scan result, when product found, should return success`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(ProductTestUtils.generateProductList())

        val result = sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode39)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given barcode scan result, when product results null, should return failure`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(null)

        whenever(
            repo.searchProductListByGlobalUniqueId(globalUniqueId = "123")
        ).thenReturn(null)

        val result = sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode39)

        assertTrue(result.isFailure)
    }

    @Test
    fun `given barcode scan result, when product results empty, should return failure`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())

        whenever(
            repo.searchProductListByGlobalUniqueId(globalUniqueId = "123")
        ).thenReturn(emptyList())

        val result = sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode39)

        assertTrue(result.isFailure)
    }

    @Test
    fun `given barcode scan result, when product found when searching by global unique id, should return success`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())

        whenever(
            repo.searchProductListByGlobalUniqueId(globalUniqueId = "123")
        ).thenReturn(ProductTestUtils.generateProductList())

        val result = sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode39)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given barcode scan result, when product found when searching by global unique id and sku, should return the global unique id`() = testBlocking {
        val skuProduct = ProductTestUtils.generateProduct(productId = 1)
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(listOf(skuProduct))

        val globalUniqueIdProduct = ProductTestUtils.generateProduct(productId = 2)
        whenever(
            repo.searchProductListByGlobalUniqueId(globalUniqueId = "123")
        ).thenReturn(listOf(globalUniqueIdProduct))

        val result = sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode39)

        assertTrue(result.isSuccess)
        assertEquals(result.getOrNull(), globalUniqueIdProduct)
    }

    @Test
    fun `given barcode scan result, when product found when searching by global unique id, should track event`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())

        whenever(
            repo.searchProductListByGlobalUniqueId(globalUniqueId = "123")
        ).thenReturn(ProductTestUtils.generateProductList())

        val result = sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode39)

        verify(analyticsTracker).track(
            AnalyticsEvent.PRODUCT_SEARCH_VIA_GLOBAL_UNIQUE_IDENTIFIER_SUCCESS
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given barcode scan result EAN8 type, when product not found, should remove check digit and try again`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())

        whenever(
            repo.searchProductListByGlobalUniqueId(globalUniqueId = "123")
        ).thenReturn(emptyList())

        val checkDigitRemover: CheckDigitRemover = mock()
        whenever(checkDigitRemoverFactory.getCheckDigitRemoverFor(any())).thenReturn(checkDigitRemover)

        sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8)

        verify(checkDigitRemover, times(2)).getSKUWithoutCheckDigit("123")
    }

    @Test
    fun `given barcode scan result EAN13 type, when product not found, should remove check digit and try again`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())

        whenever(
            repo.searchProductListByGlobalUniqueId(globalUniqueId = "123")
        ).thenReturn(emptyList())

        val checkDigitRemover: CheckDigitRemover = mock()
        whenever(checkDigitRemoverFactory.getCheckDigitRemoverFor(any())).thenReturn(checkDigitRemover)

        sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN13)

        verify(checkDigitRemover, times(2)).getSKUWithoutCheckDigit("123")
    }

    @Test
    fun `given barcode scan result UPCA type, when product not found, should remove check digit and try again`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())

        whenever(
            repo.searchProductListByGlobalUniqueId(globalUniqueId = "123")
        ).thenReturn(emptyList())

        val checkDigitRemover: CheckDigitRemover = mock()
        whenever(checkDigitRemoverFactory.getCheckDigitRemoverFor(any())).thenReturn(checkDigitRemover)

        sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCA)

        verify(checkDigitRemover, times(2)).getSKUWithoutCheckDigit("123")
    }

    @Test
    fun `given barcode scan result UPCE type, when product not found, should remove check digit and try again`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())

        whenever(
            repo.searchProductListByGlobalUniqueId(globalUniqueId = "123")
        ).thenReturn(emptyList())

        val checkDigitRemover: CheckDigitRemover = mock()
        whenever(checkDigitRemoverFactory.getCheckDigitRemoverFor(any())).thenReturn(checkDigitRemover)

        sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCE)

        verify(checkDigitRemover, times(2)).getSKUWithoutCheckDigit("123")
    }

    @Test
    fun `given product not found, when product found after removing check digit, should return success`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())
        val checkDigitRemover: CheckDigitRemover = mock()
        whenever(checkDigitRemoverFactory.getCheckDigitRemoverFor(any())).thenReturn(checkDigitRemover)
        whenever(checkDigitRemover.getSKUWithoutCheckDigit("123")).thenReturn("1234")
        whenever(
            repo.searchProductList(
                searchQuery = "1234",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(generateProductList())
        val result = sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCE)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given product not found, when product not found after removing check digit, should return failure`() = testBlocking {
        whenever(
            repo.searchProductList(
                searchQuery = "123",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())
        val checkDigitRemover: CheckDigitRemover = mock()
        whenever(checkDigitRemoverFactory.getCheckDigitRemoverFor(any())).thenReturn(checkDigitRemover)
        whenever(checkDigitRemover.getSKUWithoutCheckDigit("123")).thenReturn("1234")
        whenever(
            repo.searchProductList(
                searchQuery = "1234",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
        ).thenReturn(emptyList())
        val result = sut("123", GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCE)

        assertTrue(result.isFailure)
    }
}
