package com.woocommerce.android.aiassistant.tools

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.core.chat.ToolFailureKind
import com.woocommerce.android.aiassistant.core.chat.ToolFailureSource
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCStatsStore

class ToolFailureDiagnosticsFactoryTest {
    private val factory = ToolFailureDiagnosticsFactory(TransportDiagnosticsFactory())

    @Test
    fun `given order error with retained network response, when building transport error, then tool and transport diagnostics are populated`() {
        val response = NetworkResponse(
            409,
            """{"code":"rest_invalid_param","message":"Bearer secret"}""".toByteArray(),
            false,
            0,
            emptyList(),
        )
        val networkError = WPAPINetworkError(BaseNetworkError(VolleyError(response)))
        val error = OnChangedException(WCOrderStore.OrderError(networkError = networkError))

        val result = factory.transportError(
            toolCallId = TOOL_CALL_ID,
            toolName = "orders_get",
            error = error,
            retryable = true,
        )

        assertThat(result.retryable).isTrue()
        assertThat(result.kind).isEqualTo(ToolFailureKind.OUTCOME_UNKNOWN)
        assertThat(result.diagnostics.tool?.toolName).isEqualTo("orders_get")
        assertThat(result.diagnostics.tool?.source).isEqualTo(ToolFailureSource.TOOL_RESULT)
        assertThat(result.diagnostics.tool?.failureKind).isNull()
        assertThat(result.diagnostics.tool?.retryable).isNull()
        assertThat(result.diagnostics.transport?.httpStatus).isEqualTo(409)
        assertThat(result.diagnostics.transport?.bodySnippet).contains("rest_invalid_param")
        assertThat(result.diagnostics.transport?.bodySnippet).doesNotContain("secret")
    }

    @Test
    fun `given woo error with status in error data, when building transport error, then status is preserved without fabricated snippet`() {
        val error = OnChangedException(wooError(errorData = wooErrorData(hasStatus = true, status = 409)))

        val result = factory.transportError(
            toolCallId = TOOL_CALL_ID,
            toolName = "customers_list",
            error = error,
            retryable = false,
        )

        assertThat(result.diagnostics.tool?.toolName).isEqualTo("customers_list")
        assertThat(result.diagnostics.transport?.httpStatus).isEqualTo(409)
        assertThat(result.diagnostics.transport?.bodySnippet).isNull()
        assertThat(result.diagnostics.transport?.requestId).isNull()
        assertThat(result.diagnostics.transport?.retryAfterMs).isNull()
    }

    @Test
    fun `given woo error without status in error data, when building transport error, then transport diagnostics are absent`() {
        val error = OnChangedException(wooError(errorData = wooErrorData(hasStatus = false)))

        val result = factory.transportError(
            toolCallId = TOOL_CALL_ID,
            toolName = "customers_list",
            error = error,
            retryable = false,
        )

        assertThat(result.diagnostics.tool?.toolName).isEqualTo("customers_list")
        assertThat(result.diagnostics.transport).isNull()
    }

    @Test
    fun `given woo error with non numeric status in error data, when building transport error, then transport diagnostics are absent`() {
        val error = OnChangedException(wooError(errorData = wooErrorData(hasStatus = true, status = "409")))

        val result = factory.transportError(
            toolCallId = TOOL_CALL_ID,
            toolName = "customers_list",
            error = error,
            retryable = false,
        )

        assertThat(result.diagnostics.tool?.toolName).isEqualTo("customers_list")
        assertThat(result.diagnostics.transport).isNull()
    }

    @Test
    fun `given woo error with null status in error data, when building transport error, then transport diagnostics are absent`() {
        val error = OnChangedException(wooError(errorData = wooErrorData(hasStatus = true, status = JSONObject.NULL)))

        val result = factory.transportError(
            toolCallId = TOOL_CALL_ID,
            toolName = "customers_list",
            error = error,
            retryable = false,
        )

        assertThat(result.diagnostics.tool?.toolName).isEqualTo("customers_list")
        assertThat(result.diagnostics.transport).isNull()
    }

    @Test
    fun `given product error, when building transport error, then tool diagnostics are present and transport is absent`() {
        val error = OnChangedException(WCProductStore.ProductError(message = "network error"))

        val result = factory.transportError(
            toolCallId = TOOL_CALL_ID,
            toolName = "products_get",
            error = error,
            retryable = true,
        )

        assertLossyToolDiagnostics(result, "products_get")
    }

    @Test
    fun `given order stats error, when building transport error, then tool diagnostics are present and transport is absent`() {
        val error = OnChangedException(WCStatsStore.OrderStatsError(message = "network error"))

        val result = factory.transportError(
            toolCallId = TOOL_CALL_ID,
            toolName = "analytics_orders",
            error = error,
            retryable = true,
        )

        assertLossyToolDiagnostics(result, "analytics_orders")
    }

    @Test
    fun `given generic throwable, when building transport error, then tool diagnostics are present and transport is absent`() {
        val result = factory.transportError(
            toolCallId = TOOL_CALL_ID,
            toolName = "products_get",
            error = IllegalStateException("network error"),
            retryable = true,
        )

        assertLossyToolDiagnostics(result, "products_get")
    }

    private fun assertLossyToolDiagnostics(
        result: ToolResult.TransportError,
        toolName: String,
    ) {
        assertThat(result.diagnostics.tool?.toolName).isEqualTo(toolName)
        assertThat(result.diagnostics.tool?.source).isEqualTo(ToolFailureSource.TOOL_RESULT)
        assertThat(result.diagnostics.tool?.failureKind).isNull()
        assertThat(result.diagnostics.tool?.retryable).isNull()
        assertThat(result.diagnostics.transport).isNull()
    }

    private fun wooError(errorData: JSONObject?) = WooError(
        type = WooErrorType.API_ERROR,
        original = GenericErrorType.SERVER_ERROR,
        message = "network error",
        apiErrorCode = "rest_invalid_param",
        errorData = errorData,
    )

    private fun wooErrorData(
        hasStatus: Boolean,
        status: Any? = null,
    ) = mock<JSONObject>().apply {
        whenever(has("status")).thenReturn(hasStatus)
        whenever(opt("status")).thenReturn(status)
    }

    private companion object {
        const val TOOL_CALL_ID = "call-1"
    }
}
