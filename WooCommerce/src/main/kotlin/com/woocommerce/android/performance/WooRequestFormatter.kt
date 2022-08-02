package com.woocommerce.android.performance

import android.net.Uri
import com.automattic.android.tracks.crashlogging.FormattedUrl
import com.automattic.android.tracks.crashlogging.RequestFormatter
import okhttp3.HttpUrl
import okhttp3.Request

object WooRequestFormatter : RequestFormatter {

    override fun formatRequestUrl(request: Request): FormattedUrl {

        val newUrl = request.url.newBuilder().query(null)

        removePrivateParameters(request, newUrl)
        removeBlogId(request, newUrl)
        makeApiHostnameShorter(request, newUrl)

        val decodedUri = Uri.decode(newUrl.build().toString())
        return decodedUri.replace(
            Regex("/orders/[\\S]*?/"), "/orders/<order_id>/"
        )
    }

    private fun removePrivateParameters(request: Request, newUrl: HttpUrl.Builder) {
        if (request.url.pathSegments.contains("rest-api")) {
            request.url.queryParameterNames.forEach {
                if (it == "_method" || it == "path") {
                    newUrl.addQueryParameter(it, request.url.queryParameter(it))
                }
            }
        }
    }

    private fun makeApiHostnameShorter(request: Request, newUrl: HttpUrl.Builder) {
        if (request.url.host == "public-api.wordpress.com") {
            newUrl.host("wp_api")
        }
    }

    private fun removeBlogId(request: Request, newUrl: HttpUrl.Builder) {
        request.url.pathSegments.forEachIndexed { index, pathSegment ->
            if (pathSegment.matches(Regex("\\d{6,9}\$"))) {
                newUrl.setPathSegment(index, "<blog_id>")
            }
        }
    }

}
