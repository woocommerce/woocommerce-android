package com.woocommerce.android.aiassistant.tools

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
                transport = extractTransportDiagnostics(error),
                tool = ToolDiagnostics(
                    toolName = toolName,
                    source = ToolFailureSource.TOOL_RESULT,
                ),
            ),
        )

    private fun extractTransportDiagnostics(error: Throwable?): TransportDiagnostics? {
        val inner = (error as? OnChangedException)?.error ?: return null
        return when (inner) {
            is WCOrderStore.OrderError -> {
                val response = inner.networkError?.volleyError?.networkResponse ?: return null
                transportDiagnosticsFactory.fromRawHttp(
                    statusCode = response.statusCode,
                    headers = response.headers,
                    bodyBytes = response.data,
                )
            }
            is WooError -> {
                val status = (inner.errorData as? JSONObject)
                    ?.takeIf { it.has(STATUS_FIELD) }
                    ?.opt(STATUS_FIELD) as? Number
                status?.let { TransportDiagnostics(httpStatus = it.toInt()) }
            }
            else -> null
        }
    }

    private companion object {
        const val STATUS_FIELD = "status"
    }
}
