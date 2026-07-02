package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.util.AppLog

object JetpackTunnelRawBodyErrorLogger {
    fun buildMessage(method: String, path: String, error: WPComGsonNetworkError): String? {
        val rawBody = error.errorData?.optString("raw_body")?.takeIf { it.isNotBlank() } ?: return null
        val fields = listOfNotNull(
            "method=${sanitize(method)}",
            "path=${sanitize(path)}",
            error.volleyError?.networkResponse?.statusCode?.let { "transport_status=$it" },
            error.errorData?.opt("status")?.let { "proxy_status=${sanitize(it.toString())}" },
            "error_code=${sanitize(error.apiError)}",
            "error_message=${sanitize(error.message)}",
            "raw_body_truncated=false",
            "raw_body_snippet=${sanitize(rawBody)}"
        )
        return "Jetpack Tunnel raw_body error: ${fields.joinToString(", ")}"
    }

    fun logIfPresent(method: String, path: String, error: WPComGsonNetworkError) {
        buildMessage(method, path, error)?.let { message ->
            AppLog.w(AppLog.T.API, message)
        }
    }

    private fun sanitize(value: String?): String {
        return value.orEmpty().replace(Regex("\\s+"), " ").trim()
    }
}
