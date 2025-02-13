package org.wordpress.android.fluxc.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError

class WPAPIResponseExtTests {
    @Test
    fun `given a null response with success status, when converting, then return an error`() {
        val response = WPAPIResponse.Success<String>(null)

        val result = response.toWooPayload { "Got response: $it" }

        assertThat(result.isError).isTrue
        assertThat(result.error.type).isEqualTo(WooErrorType.EMPTY_RESPONSE)
        assertThat(result.error.original).isEqualTo(BaseRequest.GenericErrorType.UNKNOWN)
        assertThat(result.error.message).isEqualTo("Success response with empty data")
    }

    @Test
    fun `given a null response and nullable type, when converting, then return null wrapped in success`() {
        val response = WPAPIResponse.Success<String>(null)

        val result = response.toWooPayload<String, String?> { "Got response: $it" }

        assertThat(result.isError).isFalse
        assertThat(result.result).isEqualTo(null)
    }

    @Test
    fun `given a non-null success response, when converting, then map the types`() {
        val response = WPAPIResponse.Success("message")

        val result = response.toWooPayload { it.hashCode() }

        assertThat(result.isError).isFalse
        assertThat(result.result).isEqualTo("message".hashCode())
    }

    @Test
    fun `given an error, when converting, then map the error type`() {
        val error = WPAPINetworkError(BaseNetworkError(BaseRequest.GenericErrorType.SERVER_ERROR))
        val response = WPAPIResponse.Error<String>(error)

        val result = response.toWooPayload { it.hashCode() }

        assertThat(result.isError).isTrue
        assertThat(result.error.type).isEqualTo(error.toWooError().type)
        assertThat(result.error.original).isEqualTo(error.toWooError().original)
        assertThat(result.error.message).isEqualTo(error.toWooError().message)
    }

    @Test
    fun `given invalid coupon error, when converting, then map the error type`() {
        val error = WPAPINetworkError(
            BaseNetworkError(BaseRequest.GenericErrorType.UNKNOWN),
            "woocommerce_rest_invalid_coupon"
        )
        val response = WPAPIResponse.Error<String>(error)

        val result = response.toWooPayload { it.hashCode() }

        assertThat(result.isError).isTrue
        assertThat(result.error.type).isEqualTo(WooErrorType.INVALID_COUPON)
    }
}
