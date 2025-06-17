package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType

class WooPosProductSearchByIdentifierErrorMapperTest {

    private lateinit var sut: WooPosSearchByIdentifierProductErrorMapper

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierProductErrorMapper()
    }

    @Test
    fun `given invalid product id error, when invoke called, then return not found error`() {
        // GIVEN
        val error = ProductError(ProductErrorType.INVALID_PRODUCT_ID, "Invalid product ID")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.NotFound, result)
    }

    @Test
    fun `given invalid param error with message, when invoke called, then return server error with message`() {
        // GIVEN
        val message = "Invalid parameter"
        val error = ProductError(ProductErrorType.INVALID_PARAM, message)

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.ServerError(message), result)
    }

    @Test
    fun `given invalid param error without message, when invoke called, then return server error with default message`() {
        // GIVEN
        val error = ProductError(ProductErrorType.INVALID_PARAM, "")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.ServerError("Invalid parameter"), result)
    }

    @Test
    fun `given invalid review id error, when invoke called, then return not found error`() {
        // GIVEN
        val error = ProductError(ProductErrorType.INVALID_REVIEW_ID, "Invalid review ID")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.NotFound, result)
    }

    @Test
    fun `given invalid image id error, when invoke called, then return not found error`() {
        // GIVEN
        val error = ProductError(ProductErrorType.INVALID_IMAGE_ID, "Invalid image ID")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.NotFound, result)
    }

    @Test
    fun `given duplicate sku error, when invoke called, then return server error`() {
        // GIVEN
        val error = ProductError(ProductErrorType.DUPLICATE_SKU, "Duplicate SKU")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.ServerError("Duplicate SKU"), result)
    }

    @Test
    fun `given term exists error, when invoke called, then return server error`() {
        // GIVEN
        val error = ProductError(ProductErrorType.TERM_EXISTS, "Term already exists")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.ServerError("Term already exists"), result)
    }

    @Test
    fun `given invalid variation image id error, when invoke called, then return server error`() {
        // GIVEN
        val error = ProductError(ProductErrorType.INVALID_VARIATION_IMAGE_ID, "Invalid variation image ID")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.ServerError("Invalid variation image ID"), result)
    }

    @Test
    fun `given invalid min max quantity error, when invoke called, then return server error`() {
        // GIVEN
        val error = ProductError(ProductErrorType.INVALID_MIN_MAX_QUANTITY, "Invalid min/max quantity")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.ServerError("Invalid min/max quantity"), result)
    }

    @Test
    fun `given parse error, when invoke called, then return server error`() {
        // GIVEN
        val error = ProductError(ProductErrorType.PARSE_ERROR, "Parse error occurred")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.ServerError("Parse error occurred"), result)
    }

    @Test
    fun `given generic error with message, when invoke called, then return unknown error with message`() {
        // GIVEN
        val message = "Generic error occurred"
        val error = ProductError(ProductErrorType.GENERIC_ERROR, message)

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.UnknownError(message), result)
    }

    @Test
    fun `given generic error without message, when invoke called, then return unknown error with default message`() {
        // GIVEN
        val error = ProductError(ProductErrorType.GENERIC_ERROR, "")

        // WHEN
        val result = sut(error)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Error.UnknownError("Generic error occurred"), result)
    }
}
