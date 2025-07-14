package com.woocommerce.android.performance

import android.net.Uri
import com.automattic.android.tracks.crashlogging.FormattedUrl
import com.automattic.android.tracks.crashlogging.RequestFormatter
import okhttp3.HttpUrl
import okhttp3.Request

object WooRequestFormatter : RequestFormatter {
    override fun formatRequestUrl(request: Request): FormattedUrl {
        return request.url
            .newBuilder()
            .removePrivateParametersForRestApi(request)
            .replaceBlogId(request)
            .build()
            .toString()
            .let {
                Uri.decode(it)
            }
            .replaceOrderId()
            .replaceLabelId()
    }

    private fun String.replaceOrderId(): String {
        return replace(
            Regex("/orders/[\\S]*?/"),
            "/orders/<order_id>/"
        )
    }

    private fun String.replaceLabelId(): String {
        return replace(
            Regex("/label/[\\S]*?/"),
            "/label/<label_id>/"
        )
    }

    private fun HttpUrl.Builder.removePrivateParametersForRestApi(request: Request): HttpUrl.Builder {
        if (request.url.pathSegments.contains("rest-api")) {
            this.query(null)
            request.url.queryParameterNames.forEach {
                if (it == "_method" || it == "path") {
                    this.addQueryParameter(it, request.url.queryParameter(it))
                }
            }
        }
        return this
    }

    private fun HttpUrl.Builder.replaceBlogId(request: Request): HttpUrl.Builder {
        request.url.pathSegments.forEachIndexed { index, pathSegment ->
            if (pathSegment.matches(Regex("\\d{6,10}\$"))) {
                this.setPathSegment(index, "<blog_id>")
            }
        }
        return this
    }
}
