package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSearchByIdentifierRemoteTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule(UnconfinedTestDispatcher())

    private lateinit var sut: WooPosSearchByIdentifierRemote
    private val skuSearch: WooPosSearchByIdentifierSkuSearch = mock()
    private val gtinSearch: WooPosSearchByIdentifierGlobalUniqueSearch = mock()
    private val resultConverter: WooPosSearchByIdentifierResultConverter = mock()
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover = mock()

    private val testProduct = ProductTestUtils.generateProduct()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierRemote(
            skuSearch = skuSearch,
            globalUniqueIdSearch = gtinSearch,
            resultConverter = resultConverter,
            checkDigitRemover = checkDigitRemover
        )
    }

    @Test
    fun `given search result success,when searching by sku, should return success result`() = runTest {
        // GIVEN
        val identifier = "test-sku"
        val format = WooPosBarcodeFormat.FormatUPCA
        val searchResult = WooPosSearchByIdentifierResult.Success(testProduct)

        whenever(gtinSearch(identifier)).thenReturn(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound)
        )
        whenever(skuSearch(identifier)).thenReturn(searchResult)
        whenever(resultConverter.invoke(any())).thenReturn(searchResult)

        // WHEN
        val result = sut(identifier, format)

        // THEN
        assertTrue(result.isSuccess)
        assertEquals(searchResult, result)
    }

    @Test
    fun `when GTIN search succeeds, should return success result and cancel SKU search`() = runTest {
        // GIVEN
        val identifier = "test-gtin"
        val format = WooPosBarcodeFormat.FormatUPCA
        val searchResult = WooPosSearchByIdentifierResult.Success(testProduct)

        whenever(gtinSearch(identifier)).thenReturn(searchResult)
        whenever(resultConverter.invoke(any())).thenReturn(searchResult)

        // WHEN
        val result = sut(identifier, format)

        // THEN
        assertTrue(result.isSuccess)
        assertEquals(searchResult, result)
    }

    @Test
    fun `when both searches fail, should return failure result`() = runTest {
        // GIVEN
        val identifier = "test-identifier"
        val format = WooPosBarcodeFormat.FormatUPCA
        val failureResult = WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound)

        whenever(gtinSearch(identifier)).thenReturn(failureResult)
        whenever(skuSearch(identifier)).thenReturn(failureResult)
        whenever(resultConverter.invoke(any())).thenReturn(failureResult)
        whenever(checkDigitRemover(identifier, format)).thenReturn(identifier)

        // WHEN
        val result = sut(identifier, format)

        // THEN
        assertTrue(result.isFailure)
        assertEquals(failureResult, result)
    }
}
