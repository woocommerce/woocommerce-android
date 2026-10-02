package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import androidx.annotation.VisibleForTesting
import org.wordpress.android.fluxc.network.SensitiveDataSanitizer.sanitize
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseExcerpt
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.util.AppLog

object JetpackTunnelRawBodyErrorLogger {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun buildMessage(method: String, path: String, error: WPComGsonNetworkError): String? {
        val rawBody = error.errorData?.optString("raw_body")?.takeIf { it.isNotBlank() } ?: return null
        val fields = listOfNotNull(
            "method=$method",
            "path=${sanitize(path)}",
            error.volleyError?.networkResponse?.statusCode?.let { "transport_status=$it" },
            error.errorData?.opt("status")?.let { "proxy_status=${sanitize(it.toString())}" },
            "error_code=${sanitize(error.apiError)}",
            "error_message=${sanitize(error.message)}",
            "raw_body_excerpt=${UnexpectedStoreResponseExcerpt.from(rawBody).orEmpty()}"
        )
        return "Jetpack Tunnel raw_body error: ${fields.joinToString(", ")}"
    }

    fun logIfPresent(method: String, path: String, error: WPComGsonNetworkError) {
        buildMessage(method, path, error)?.let { message ->
            AppLog.w(AppLog.T.API, message)
        }
    }
}
