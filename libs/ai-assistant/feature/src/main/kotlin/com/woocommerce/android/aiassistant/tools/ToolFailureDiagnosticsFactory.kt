package com.woocommerce.android.aiassistant.tools

import com.android.volley.NetworkResponse
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.core.chat.Diagnostics
import com.woocommerce.android.aiassistant.core.chat.ToolDiagnostics
import com.woocommerce.android.aiassistant.core.chat.ToolFailureKind
import com.woocommerce.android.aiassistant.core.chat.ToolFailureSource
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.TransportDiagnostics
import org.json.JSONObject
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

internal class ToolFailureDiagnosticsFactory @Inject constructor(
    private val transportDiagnosticsFactory: TransportDiagnosticsFactory,
) {
    fun transportError(
        toolCallId: String,
        toolName: String,
        error: Throwable?,
        retryable: Boolean,
        kind: ToolFailureKind = ToolFailureKind.OUTCOME_UNKNOWN,
    ): ToolResult.TransportError =
        ToolResult.TransportError(
            toolCallId = toolCallId,
            retryable = retryable,
            kind = kind,
            diagnostics = Diagnostics(
                transport = error.transportDiagnostics(),
                tool = ToolDiagnostics(
                    toolName = toolName,
                    source = ToolFailureSource.TOOL_RESULT,
                ),
            ),
        )

    private fun Throwable?.transportDiagnostics(): TransportDiagnostics? =
        when (this) {
            is OnChangedException -> error.transportDiagnostics()
            else -> null
        }

    private fun Any.transportDiagnostics(): TransportDiagnostics? =
        when (this) {
            is WCOrderStore.OrderError ->
                networkError
                    ?.volleyError
                    ?.networkResponse
                    ?.transportDiagnostics()
            is WooError -> errorData.transportDiagnostics()
            else -> null
        }

    private fun NetworkResponse.transportDiagnostics(): TransportDiagnostics? =
        transportDiagnosticsFactory.fromRawHttp(
            statusCode = statusCode,
            headers = headers,
            bodyBytes = data,
        )

    private fun JSONObject?.transportDiagnostics(): TransportDiagnostics? {
        val status = this
            ?.takeIf { it.has(STATUS_FIELD) }
            ?.opt(STATUS_FIELD) as? Number
        return status?.let { TransportDiagnostics(httpStatus = it.toInt()) }
    }

    private companion object {
        const val STATUS_FIELD = "status"
    }
}
