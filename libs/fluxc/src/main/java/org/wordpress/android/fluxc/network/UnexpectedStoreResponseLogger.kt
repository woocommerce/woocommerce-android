package org.wordpress.android.fluxc.network

import org.wordpress.android.util.AppLog
import java.util.Locale

object UnexpectedStoreResponseLogger {
    fun buildMessage(response: UnexpectedStoreResponse): String = "Unexpected store response: ${buildFields(response)}"

    fun buildFields(response: UnexpectedStoreResponse): String = listOf(
        "kind=${response.kind.name.lowercase(Locale.ROOT)}",
        "status=${response.statusCode}",
        "content_type=${response.contentType.orEmpty()}",
        "request=${response.requestType}",
        "excerpt=${response.excerpt.orEmpty()}"
    ).joinToString(", ")

    @JvmStatic
    fun log(response: UnexpectedStoreResponse) {
        AppLog.w(AppLog.T.API, buildMessage(response))
    }
}
