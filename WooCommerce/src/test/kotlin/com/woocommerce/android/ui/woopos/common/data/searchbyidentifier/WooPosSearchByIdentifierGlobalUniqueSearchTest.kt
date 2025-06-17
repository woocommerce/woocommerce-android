package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore

class WooPosSearchByIdentifierGlobalUniqueSearchTest {

    private lateinit var sut: WooPosSearchByIdentifierGlobalUniqueSearch
    private val selectedSite: SelectedSite = mock()
    private val productStore: WCProductStore = mock()
    private val site: SiteModel = mock()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierGlobalUniqueSearch(selectedSite, productStore)
        whenever(selectedSite.get()).thenReturn(site)
    }

    @Test
    fun `given successful search with products, when invoke called, then return success with first product`() =
        runTest {
            // GIVEN
            val globalUniqueId = "GU123456789"
            val wcProduct = ProductTestUtils.generateWCProductModel()
            val expectedProduct = wcProduct.toAppModel()
            val searchResult = WCProductStore.ProductSearchResult(
                products = listOf(wcProduct),
                canLoadMore = false
            )
            val result = WooResult(searchResult)
            whenever(
                productStore.searchProducts(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            ).thenReturn(result)

            // WHEN
            val actualResult = sut(globalUniqueId)

            // THEN
            assertTrue(actualResult is WooPosSearchByIdentifierResult.Success)
            val successResult = actualResult as WooPosSearchByIdentifierResult.Success
            assertEquals(expectedProduct.remoteId, successResult.product.remoteId)
            assertEquals(expectedProduct.name, successResult.product.name)
        }

    @Test
    fun `given successful search with no products, when invoke called, then return failure with product not found`() =
        runTest {
            // GIVEN
            val globalUniqueId = "NONEXISTENT-GU"
            val searchResult = WCProductStore.ProductSearchResult(
                products = emptyList(),
                canLoadMore = false
            )
            val result = WooResult(searchResult)
            whenever(
                productStore.searchProducts(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            ).thenReturn(result)

            // WHEN
            val actualResult = sut(globalUniqueId)

            // THEN
            assertEquals(
                WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
                actualResult
            )
        }

    @Test
    fun `given network error, when invoke called, then return failure with network error`() = runTest {
        // GIVEN
        val globalUniqueId = "ERROR-GU"
        val error = mock<WooError> {
            on { type }.thenReturn(WooErrorType.TIMEOUT)
        }
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = error)
        whenever(
            productStore.searchProducts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError),
            actualResult
        )
    }

    @Test
    fun `given null model result, when invoke called, then return failure with unknown error`() = runTest {
        // GIVEN
        val globalUniqueId = "NULL-MODEL-GU"
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(model = null)
        whenever(
            productStore.searchProducts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ServerError(
                    "Empty response from server for global unique ID: $globalUniqueId"
                )
            ),
            actualResult
        )
    }

    @Test
    fun `given api error, when invoke called, then return failure with server error`() = runTest {
        // GIVEN
        val globalUniqueId = "ERROR-GU"
        val error = mock<WooError> {
            on { type }.thenReturn(WooErrorType.API_ERROR)
            on { message }.thenReturn("API error occurred")
        }
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = error)
        whenever(
            productStore.searchProducts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ServerError("API error occurred")
            ),
            actualResult
        )
    }

    @Test
    fun `given invalid id error, when invoke called, then return failure with not found error`() = runTest {
        // GIVEN
        val globalUniqueId = "INVALID-GU"
        val error = mock<WooError> {
            on { type }.thenReturn(WooErrorType.INVALID_ID)
        }
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = error)
        whenever(
            productStore.searchProducts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
            actualResult
        )
    }

    @Test
    fun `given generic error, when invoke called, then return failure with unknown error`() = runTest {
        // GIVEN
        val globalUniqueId = "GENERIC-ERROR-GU"
        val error = mock<WooError> {
            on { type }.thenReturn(WooErrorType.GENERIC_ERROR)
            on { message }.thenReturn("Generic error occurred")
        }
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = error)
        whenever(
            productStore.searchProducts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError("Generic error occurred")
            ),
            actualResult
        )
    }

    @Test
    fun `given authorization required error, when invoke called, then return failure with server error`() = runTest {
        // GIVEN
        val globalUniqueId = "AUTH-ERROR-GU"
        val error = mock<WooError> {
            on { type }.thenReturn(WooErrorType.AUTHORIZATION_REQUIRED)
        }
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = error)
        whenever(
            productStore.searchProducts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ServerError("Authorization required")
            ),
            actualResult
        )
    }

    @Test
    fun `given api not found error, when invoke called, then return failure with not found error`() = runTest {
        // GIVEN
        val globalUniqueId = "NOT-FOUND-GU"
        val error = mock<WooError> {
            on { type }.thenReturn(WooErrorType.API_NOT_FOUND)
        }
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = error)
        whenever(
            productStore.searchProducts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
            actualResult
        )
    }

    @Test
    fun `given empty response error, when invoke called, then return failure with not found error`() = runTest {
        // GIVEN
        val globalUniqueId = "EMPTY-RESPONSE-GU"
        val error = mock<WooError> {
            on { type }.thenReturn(WooErrorType.EMPTY_RESPONSE)
        }
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = error)
        whenever(
            productStore.searchProducts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
            actualResult
        )
    }

    @Test
    fun `given invalid response error, when invoke called, then return failure with server error`() = runTest {
        // GIVEN
        val globalUniqueId = "INVALID-RESPONSE-GU"
        val error = mock<WooError> {
            on { type }.thenReturn(WooErrorType.INVALID_RESPONSE)
            on { message }.thenReturn("Invalid response from server")
        }
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = error)
        whenever(
            productStore.searchProducts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.ServerError("Invalid response from server")
            ),
            actualResult
        )
    }
}
