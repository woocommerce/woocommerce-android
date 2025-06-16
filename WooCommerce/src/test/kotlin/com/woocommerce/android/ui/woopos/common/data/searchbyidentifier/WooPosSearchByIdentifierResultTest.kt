package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class WooPosSearchByIdentifierResultTest {

    @Test
    fun `given Success result, when checking isSuccess, then return true`() {
        // GIVEN
        val product: Product = mock()
        val result = WooPosSearchByIdentifierResult.Success(product)

        // WHEN & THEN
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
    }

    @Test
    fun `given VariationSuccess result, when checking isSuccess, then return true`() {
        // GIVEN
        val variation: ProductVariation = mock()
        val parentProduct: Product = mock()
        val result = WooPosSearchByIdentifierResult.VariationSuccess(variation, parentProduct)

        // WHEN & THEN
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
    }

    @Test
    fun `given Failure result, when checking isFailure, then return true`() {
        // GIVEN
        val result = WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.ProductNotFound)

        // WHEN & THEN
        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `given NetworkError failure, when checking isFailure, then return true`() {
        // GIVEN
        val result = WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError)

        // WHEN & THEN
        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `given UnsupportedProduct failure, when checking isFailure, then return true`() {
        // GIVEN
        val result = WooPosSearchByIdentifierResult.Failure(
            WooPosSearchByIdentifierResult.Error.UnsupportedProduct("Test Product")
        )

        // WHEN & THEN
        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `given UnknownError failure, when checking isFailure, then return true`() {
        // GIVEN
        val result = WooPosSearchByIdentifierResult.Failure(
            WooPosSearchByIdentifierResult.Error.UnknownError("Test error message")
        )

        // WHEN & THEN
        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }
}
