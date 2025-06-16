package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSearchByIdentifierResultConverterTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule(UnconfinedTestDispatcher())

    private lateinit var sut: WooPosSearchByIdentifierResultConverter
    private val productsCache: WooPosProductsCache = mock()
    private val variationProcess: WooPosSearchByIdentifierVariationProcess = mock()

    private val testProduct = ProductTestUtils.generateProduct()
    private val testVariation = ProductTestUtils.generateProductVariation()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierResultConverter(productsCache, variationProcess)
    }

    @Test
    fun `given regular product success result, when converting, should add to cache and return result`() = runTest {
        // GIVEN
        val successResult = WooPosSearchByIdentifierResult.Success(testProduct)
        val searchFunction: suspend () -> WooPosSearchByIdentifierResult = { successResult }

        // WHEN
        val result = sut(searchFunction)

        // THEN
        assertThat(result).isInstanceOf(WooPosSearchByIdentifierResult.Success::class.java)
        assertThat((result as WooPosSearchByIdentifierResult.Success).product).isEqualTo(testProduct)
        verify(productsCache).addAll(listOf(testProduct))
    }

    @Test
    fun `given variation product success result, when converting, should process variation`() = runTest {
        // GIVEN
        val variationProduct = testProduct.copy(type = "variation")
        val successResult = WooPosSearchByIdentifierResult.Success(variationProduct)
        val variationSuccessResult = WooPosSearchByIdentifierResult.VariationSuccess(testVariation, testProduct)
        val searchFunction: suspend () -> WooPosSearchByIdentifierResult = { successResult }

        whenever(variationProcess(variationProduct)).thenReturn(variationSuccessResult)

        // WHEN
        val result = sut(searchFunction)

        // THEN
        assertThat(result).isInstanceOf(WooPosSearchByIdentifierResult.VariationSuccess::class.java)
        verify(variationProcess).invoke(variationProduct)
    }

    @Test
    fun `given variation success result, when converting, should add parent product to cache and return result`() = runTest {
        // GIVEN
        val variationSuccessResult = WooPosSearchByIdentifierResult.VariationSuccess(testVariation, testProduct)
        val searchFunction: suspend () -> WooPosSearchByIdentifierResult = { variationSuccessResult }

        // WHEN
        val result = sut(searchFunction)

        // THEN
        assertThat(result).isInstanceOf(WooPosSearchByIdentifierResult.VariationSuccess::class.java)
        assertThat(result).isEqualTo(variationSuccessResult)
        verify(productsCache).addAll(listOf(testProduct))
    }

    @Test
    fun `given failure result, when converting, should return failure unchanged`() = runTest {
        // GIVEN
        val failureResult = WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound)
        val searchFunction: suspend () -> WooPosSearchByIdentifierResult = { failureResult }

        // WHEN
        val result = sut(searchFunction)

        // THEN
        assertThat(result).isInstanceOf(WooPosSearchByIdentifierResult.Failure::class.java)
        assertThat(result).isEqualTo(failureResult)
    }
}
