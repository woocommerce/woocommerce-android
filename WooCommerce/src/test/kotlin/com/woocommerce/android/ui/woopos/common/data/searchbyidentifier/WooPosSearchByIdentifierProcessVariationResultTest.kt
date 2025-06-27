package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosSearchByIdentifierProcessVariationResultTest {
    private lateinit var sut: WooPosSearchByIdentifierProcessVariationResult
    private val variationFetch: WooPosSearchByIdentifierVariationFetch = mock()
    private val productGetOrFetch: WooPosSearchByIdentifierProductGetOrFetch = mock()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierProcessVariationResult(variationFetch, productGetOrFetch)
    }

    @Test
    fun `given product with valid parent id and successful fetches, when invoke called, then return variation success`() = runTest {
        // GIVEN
        val parentId = 123L
        val variationId = 456L
        val product: Product = mock {
            on { this.parentId }.thenReturn(parentId)
            on { remoteId }.thenReturn(variationId)
        }
        val parentProduct: Product = mock()
        val variation: ProductVariation = mock()

        runBlocking {
            whenever(
                variationFetch.invoke(variationId, parentId)
            ).thenReturn(WooPosSearchByIdentifierVariationFetch.VariationFetchResult.Success(variation))
            whenever(productGetOrFetch.invoke(parentId)).thenReturn(
                WooPosSearchByIdentifierResult.Success(parentProduct)
            )
        }

        // WHEN
        val result = sut(product)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.VariationSuccess(variation, parentProduct), result)
    }

    @Test
    fun `given product with invalid parent id, when invoke called, then return product not found failure`() = runTest {
        // GIVEN
        val product: Product = mock {
            on { parentId }.thenReturn(0L)
            on { remoteId }.thenReturn(456L)
        }

        // WHEN
        val result = sut(product)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
            result
        )
    }

    @Test
    fun `given variation fetch fails, when invoke called, then return first failure`() = runTest {
        // GIVEN
        val parentId = 123L
        val variationId = 456L
        val product: Product = mock {
            on { this.parentId }.thenReturn(parentId)
            on { remoteId }.thenReturn(variationId)
        }

        runBlocking {
            whenever(
                variationFetch.invoke(variationId, parentId)
            ).thenReturn(
                WooPosSearchByIdentifierVariationFetch.VariationFetchResult.Failure(
                    WooPosSearchByIdentifierResult.Error.NetworkError
                )
            )
            whenever(
                productGetOrFetch.invoke(parentId)
            ).thenReturn(WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError))
        }

        // WHEN
        val result = sut(product)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError),
            result
        )
    }

    @Test
    fun `given parent product fetch fails, when invoke called, then return first failure`() = runTest {
        // GIVEN
        val parentId = 123L
        val variationId = 456L
        val product: Product = mock {
            on { this.parentId }.thenReturn(parentId)
            on { remoteId }.thenReturn(variationId)
        }
        val variation: ProductVariation = mock()
        val expectedFailure = WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError)

        runBlocking {
            whenever(
                variationFetch.invoke(variationId, parentId)
            ).thenReturn(WooPosSearchByIdentifierVariationFetch.VariationFetchResult.Success(variation))
            whenever(productGetOrFetch.invoke(parentId)).thenReturn(expectedFailure)
        }

        // WHEN
        val result = sut(product)

        // THEN
        assertEquals(expectedFailure, result)
    }

    @Test
    fun `given both fetches fail, when invoke called, then return first failure from variation result`() = runTest {
        // GIVEN
        val parentId = 123L
        val variationId = 456L
        val product: Product = mock {
            on { this.parentId }.thenReturn(parentId)
            on { remoteId }.thenReturn(variationId)
        }

        val variationResult = WooPosSearchByIdentifierVariationFetch.VariationFetchResult.Failure(
            WooPosSearchByIdentifierResult.Error.UnknownError("Variation not found for ID: $variationId")
        )
        val parentProductResult = WooPosSearchByIdentifierResult.Failure(
            WooPosSearchByIdentifierResult.Error.NetworkError
        )
        val expectedFailure = WooPosSearchByIdentifierResult.Failure(
            WooPosSearchByIdentifierResult.Error.UnknownError("Variation not found for ID: $variationId")
        )

        runBlocking {
            whenever(variationFetch.invoke(variationId, parentId)).thenReturn(variationResult)
            whenever(productGetOrFetch.invoke(parentId)).thenReturn(parentProductResult)
        }

        // WHEN
        val result = sut(product)

        // THEN
        assertEquals(expectedFailure, result)
    }
}
