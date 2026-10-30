package com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus

import com.woocommerce.android.WooException
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UnexpectedStoreResponse
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

class IsServiceOutageErrorTest {
    private val sut = IsServiceOutageError()

    @Test
    fun `when the connect server returned a 5xx, then it is an outage`() {
        listOf(500, 502, 503, 504).forEach { status ->
            val error = connectServerError(code = "wcc_server_error_response", upstreamStatus = status)

            assertThat(sut(WooException(error))).isTrue()
        }
    }

    @Test
    fun `when the connect server rejected the purchase with a 4xx, then it is not an outage`() {
        val error = connectServerError(
            code = "wcc_server_error_response",
            message = "Error: The WooCommerce Shipping server returned: Payment Required " +
                "Your card was declined. ( 402 )",
            upstreamStatus = 402
        )

        assertThat(sut(WooException(error))).isFalse()
    }

    @Test
    fun `when the connect server could not be reached, then it is an outage`() {
        val error = connectServerError(code = "wcc_server_error", upstreamStatus = 0)

        assertThat(sut(WooException(error))).isTrue()
    }

    @Test
    fun `when a connect server error has no upstream status, then it is an outage`() {
        listOf("wcc_server_error", "wcc_server_error_response", "wcc_server_empty_response").forEach { code ->
            val error = connectServerError(code = code, upstreamStatus = null)

            assertThat(sut(WooException(error))).isTrue()
        }
    }

    @Test
    fun `when the store itself failed without a connect server error, then it is not an outage`() {
        val errors = listOf(
            wooError(original = GenericErrorType.SERVER_ERROR, apiErrorCode = "internal_server_error"),
            wooError(errorData = mock<JSONObject> { on { optInt("status") } doReturn 503 }),
            wooError(message = "", unexpectedStoreResponse = unexpectedStoreResponse(503))
        )

        errors.forEach { error ->
            assertThat(sut(WooException(error))).isFalse()
        }
    }

    @Test
    fun `when a pass through connect server error has a 4xx status, then it is not an outage`() {
        val error = wooError(
            original = GenericErrorType.SERVER_ERROR,
            apiErrorCode = "missing_upsdap_terms_of_service_acceptance",
            errorData = mock<JSONObject> { on { optInt("response_status_code") } doReturn 400 }
        )

        assertThat(sut(WooException(error))).isFalse()
    }

    @Test
    fun `when error is a regular api error, then it is not an outage`() {
        val errorData = mock<JSONObject> { on { optInt("status") } doReturn 400 }
        val error = wooError(message = "Invalid phone number", errorData = errorData)

        assertThat(sut(WooException(error))).isFalse()
    }

    @Test
    fun `when exception is not a WooException, then it is not an outage`() {
        assertThat(sut(Exception("Error: The WooCommerce Shipping server returned HTTP code: 503"))).isFalse()
    }

    private fun connectServerError(
        code: String,
        message: String = "Error: The WooCommerce Shipping server returned: Internal Server Error " +
            "An internal server error occurred ( 500 )",
        upstreamStatus: Int?
    ) = wooError(
        original = GenericErrorType.SERVER_ERROR,
        apiErrorCode = code,
        message = message,
        errorData = upstreamStatus?.let { status ->
            mock<JSONObject> { on { optInt("response_status_code") } doReturn status }
        }
    )

    private fun wooError(
        original: GenericErrorType = GenericErrorType.UNKNOWN,
        apiErrorCode: String? = null,
        message: String = "Error",
        errorData: JSONObject? = null,
        unexpectedStoreResponse: UnexpectedStoreResponse? = null
    ) = WooError(
        type = WooErrorType.API_ERROR,
        original = original,
        message = message,
        apiErrorCode = apiErrorCode,
        errorData = errorData,
        unexpectedStoreResponse = unexpectedStoreResponse
    )

    private fun unexpectedStoreResponse(statusCode: Int) = UnexpectedStoreResponse(
        kind = UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE,
        statusCode = statusCode,
        contentType = "text/html",
        requestType = "POST /wcshipping/v1/label/purchase",
        excerpt = "Service Unavailable"
    )
}
