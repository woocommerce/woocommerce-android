package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.products.ProductTestUtils
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
    private val globalUniqueIdSearch: WooPosSearchByIdentifierGlobalUniqueSearch = mock()
    private val resultConverter: WooPosSearchByIdentifierResultConverter = mock()

    private val testProduct = ProductTestUtils.generateProduct()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierRemote(
            globalUniqueIdSearch = globalUniqueIdSearch,
            resultConverter = resultConverter
        )
    }

    @Test
    fun `given search result success, when searching by global unique id, should return success result`() = runTest {
        // GIVEN
        val identifier = "test-gtin"
        val searchResult = WooPosSearchByIdentifierResult.Success(testProduct)

        whenever(globalUniqueIdSearch(identifier)).thenReturn(searchResult)
        whenever(resultConverter.invoke(any())).thenReturn(searchResult)

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result.isSuccess)
        assertEquals(searchResult, result)
    }

    @Test
    fun `when search fails, should return failure result`() = runTest {
        // GIVEN
        val identifier = "test-identifier"
        val failureResult = WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound)

        whenever(globalUniqueIdSearch(identifier)).thenReturn(failureResult)
        whenever(resultConverter.invoke(any())).thenReturn(failureResult)

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result.isFailure)
        assertEquals(failureResult, result)
    }

    @Test
    fun `when network error occurs, should return network error`() = runTest {
        // GIVEN
        val identifier = "test-identifier"
        val networkError = WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError)

        whenever(globalUniqueIdSearch(identifier)).thenReturn(networkError)
        whenever(resultConverter.invoke(any())).thenReturn(networkError)

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result.isFailure)
        assertEquals(networkError, result)
    }
}
