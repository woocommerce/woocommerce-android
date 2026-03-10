package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.toWooPosVariation
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
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
    private val variationProcess: WooPosSearchByIdentifierProcessVariationResult = mock()
    private val variationMapper: WooPosVariationMapper = mock()

    private val testProduct = generateWooPosProduct()
    private val testProductVariation = ProductTestUtils.generateProductVariation()
    private val testVariation by lazy { testProductVariation.toWooPosVariation(variationMapper) }

    @Before
    fun setup() {
        whenever(variationMapper.fromProductVariation(any())).thenReturn(
            WooPosVariation(
                remoteVariationId = 1L,
                remoteProductId = 1L,
                globalUniqueId = "test-unique-id",
                type = WooPosVariation.WooPosVariationType.VARIATION,
                price = java.math.BigDecimal("10.0"),
                image = null,
                attributes = emptyList(),
                isVisible = true,
                isDownloadable = false
            )
        )
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
        val variationProduct = testProduct.copy(
            type = WooPosProductModel.WooPosProductType.Variation
        )
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
