package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.chat.TransportDiagnostics
import okhttp3.Response
import javax.inject.Inject

internal class TransportDiagnosticsFactory @Inject constructor() {
    fun from(response: Response?): TransportDiagnostics? {
        response ?: return null
        return TransportDiagnostics(
            httpStatus = response.code,
            requestId = response.firstAllowlistedRequestId(),
        )
    }

    private fun Response.firstAllowlistedRequestId(): String? =
        REQUEST_ID_HEADER_NAMES
            .firstNotNullOfOrNull { name -> header(name)?.takeIf { it.isNotBlank() } }

    private companion object {
        val REQUEST_ID_HEADER_NAMES = listOf(
            "X-Request-Id",
            "X-Request-ID",
            "X-WP-Request-ID",
            "X-WP-Request-Id",
        )
    }
}
